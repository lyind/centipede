/*
 * Copyright (C) 2017  Jonas Zeiger <jonas.zeiger@talpidae.net>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.talpidae.centipede.util.process;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.IntByReference;
import lombok.Getter;
import lombok.val;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.sun.jna.platform.win32.WinNT.PROCESS_TERMINATE;


public class ProcessUtil
{
    @Getter
    private static final String systemName = System.getProperty("os.name", "unknown").toLowerCase();

    @Getter
    private static final Platform platform = detectPlatform();


    // Based on a stack-overflow answer from:
    //   https://stackoverflow.com/questions/4750470/how-to-get-pid-of-process-ive-just-started-within-java-program
    public static long getProcessID(Process process)
    {
        final long pid;
        try
        {
            if (platform == Platform.WINDOWS)
            {
                // windows
                val field = process.getClass().getDeclaredField("handle");
                field.setAccessible(true);

                val handle = field.getLong(process);
                Kernel32 kernel = Kernel32.INSTANCE;
                WinNT.HANDLE hand = new WinNT.HANDLE();
                hand.setPointer(Pointer.createConstant(handle));
                pid = kernel.GetProcessId(hand);

                field.setAccessible(false);
            }
            else if (platform == Platform.UNIX)
            {
                // UNIX based
                val field = process.getClass().getDeclaredField("pid");
                field.setAccessible(true);

                pid = field.getLong(process);

                field.setAccessible(false);
            }
            else
            {
                // TODO: Support Java 9 here
                return -1;
            }
        }
        catch (Exception ex)
        {
            return -1;
        }

        return pid;
    }


    /**
     * Terminate a process forcibly.
     */
    public static boolean terminateProcess(long pid, boolean forcibly)
    {
        if (platform == Platform.WINDOWS)
        {
            val kernel32 = Kernel32.INSTANCE;
            val handle = kernel32.OpenProcess(PROCESS_TERMINATE, false, (int) pid);
            if (handle != null)
            {
                if (forcibly)
                {
                    return kernel32.TerminateProcess(handle, 1);
                }
                else
                {
                    val user32 = User32.INSTANCE;

                    val outPidRef = new IntByReference();
                    AtomicBoolean messagePosted = new AtomicBoolean(false);

                    // post WM_CLOSE to each top-level window of the process
                    if (user32.EnumWindows((WinDef.HWND hWnd, Pointer data) ->
                    {
                        outPidRef.setValue(-1);
                        user32.GetWindowThreadProcessId(hWnd, outPidRef);

                        if (outPidRef.getValue() == pid)
                        {
                            user32.PostMessage(hWnd, WinUser.WM_CLOSE, new WinDef.WPARAM(), new WinDef.LPARAM());

                            messagePosted.lazySet(true);
                        }

                        return true;
                    }, Pointer.NULL))
                    {
                        return messagePosted.get();
                    }
                }
            }
        }
        else if (platform == Platform.UNIX)
        {
            return LibCLinux.INSTANCE.kill((int) pid, forcibly ? LibCLinux.SIGKILL : LibCLinux.SIGTERM) == 0;
        }

        return false;
    }


    private static Platform detectPlatform()
    {
        if (systemName.startsWith("windows"))
        {
            return Platform.WINDOWS;
        }

        return Platform.UNIX;
    }
}
