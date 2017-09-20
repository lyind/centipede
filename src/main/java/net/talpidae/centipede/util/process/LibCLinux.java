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

import com.sun.jna.Native;
import com.sun.jna.platform.unix.LibC;


public interface LibCLinux extends LibC
{
    String NAME = "c";
    LibCLinux INSTANCE = Native.loadLibrary(NAME, LibCLinux.class);

    // errors (for Linux)
    // @formatter:off
    int EPERM         =   1;     /* Operation not permitted */
    int ENOENT        =   2;     /* No such file or directory */
    int ESRCH         =   3;     /* No such process */
    int EINTR         =   4;     /* Interrupted system call */
    int EIO           =   5;     /* I/O error */
    int ENXIO         =   6;     /* No such device or address */
    int E2BIG         =   7;     /* Arg list too long */
    int ENOEXEC       =   8;     /* Exec format error */
    int EBADF         =   9;     /* Bad file number */
    int ECHILD        =  10;     /* No child processes */
    int EAGAIN        =  11;     /* Try again */
    int ENOMEM        =  12;     /* Out of memory */
    int EACCES        =  13;     /* Permission denied */
    int EFAULT        =  14;     /* Bad address */
    int ENOTBLK       =  15;     /* Block device required */
    int EBUSY         =  16;     /* Device or resource busy */
    int EEXIST        =  17;     /* File exists */
    int EXDEV         =  18;     /* Cross-device link */
    int ENODEV        =  19;     /* No such device */
    int ENOTDIR       =  20;     /* Not a directory */
    int EISDIR        =  21;     /* Is a directory */
    int EINVAL        =  22;     /* Invalid argument */
    int ENFILE        =  23;     /* File table overflow */
    int EMFILE        =  24;     /* Too many open files */
    int ENOTTY        =  25;     /* Not a typewriter */
    int ETXTBSY       =  26;     /* Text file busy */
    int EFBIG         =  27;     /* File too large */
    int ENOSPC        =  28;     /* No space left on device */
    int ESPIPE        =  29;     /* Illegal seek */
    int EROFS         =  30;     /* Read-only file system */
    int EMLINK        =  31;     /* Too many links */
    int EPIPE         =  32;     /* Broken pipe */
    int EDOM          =  33;     /* Math argument out of domain of func */
    int ERANGE        =  34;     /* Math result not representable */
    int EDEADLK       =  35;     /* Resource deadlock would occur */
    int ENAMETOOLONG  =  36;     /* File name too long */
    int ENOLCK        =  37;     /* No record locks available */
    int ENOSYS        =  38;     /* Function not implemented */
    int ENOTEMPTY     =  39;     /* Directory not empty */
    int ELOOP         =  40;     /* Too many symbolic links encountered */
    int EWOULDBLOCK   =  EAGAIN; /* Operation would block */
    int ENOMSG        =  42;     /* No message of desired type */
    int EIDRM         =  43;     /* Identifier removed */
    int ECHRNG        =  44;     /* Channel number out of range */
    int EL2NSYNC      =  45;     /* Level 2 not synchronized */
    int EL3HLT        =  46;     /* Level 3 halted */
    int EL3RST        =  47;     /* Level 3 reset */
    int ELNRNG        =  48;     /* Link number out of range */
    int EUNATCH       =  49;     /* Protocol driver not attached */
    int ENOCSI        =  50;     /* No CSI structure available */
    int EL2HLT        =  51;     /* Level 2 halted */
    int EBADE         =  52;     /* Invalid exchange */
    int EBADR         =  53;     /* Invalid request descriptor */
    int EXFULL        =  54;     /* Exchange full */
    int ENOANO        =  55;     /* No anode */
    int EBADRQC       =  56;     /* Invalid request code */
    int EBADSLT       =  57;     /* Invalid slot */
    int EDEADLOCK     =  EDEADLK;
    int EBFONT        =  59;     /* Bad font file format */
    int ENOSTR        =  60;     /* Device not a stream */
    int ENODATA       =  61;     /* No data available */
    int ETIME         =  62;     /* Timer expired */
    int ENOSR         =  63;     /* Out of streams resources */
    int ENONET        =  64;     /* Machine is not on the network */
    int ENOPKG        =  65;     /* Package not installed */
    int EREMOTE       =  66;     /* Object is remote */
    int ENOLINK       =  67;     /* Link has been severed */
    int EADV          =  68;     /* Advertise error */
    int ESRMNT        =  69;     /* Srmount error */
    int ECOMM         =  70;     /* Communication error on send */
    int EPROTO        =  71;     /* Protocol error */
    int EMULTIHOP     =  72;     /* Multihop attempted */
    int EDOTDOT       =  73;     /* RFS specific error */
    int EBADMSG       =  74;     /* Not a data message */
    int EOVERFLOW     =  75;     /* Value too large for defined data type */
    int ENOTUNIQ      =  76;     /* Name not unique on network */
    int EBADFD        =  77;     /* File descriptor in bad state */
    int EREMCHG       =  78;     /* Remote address changed */
    int ELIBACC       =  79;     /* Can not access a needed shared library */
    int ELIBBAD       =  80;     /* Accessing a corrupted shared library */
    int ELIBSCN       =  81;     /* .lib section in a.out corrupted */
    int ELIBMAX       =  82;     /* Attempting to link in too many shared libraries */
    int ELIBEXEC      =  83;     /* Cannot exec a shared library directly */
    int EILSEQ        =  84;     /* Illegal byte sequence */
    int ERESTART      =  85;     /* Interrupted system call should be restarted */
    int ESTRPIPE      =  86;     /* Streams pipe error */
    int EUSERS        =  87;     /* Too many users */
    int ENOTSOCK      =  88;     /* Socket operation on non-socket */
    int EDESTADDRREQ  =  89;     /* Destination address required */
    int EMSGSIZE      =  90;     /* Message too long */
    int EPROTOTYPE    =  91;     /* Protocol wrong type for socket */
    int ENOPROTOOPT   =  92;     /* Protocol not available */
    int EPROTONOSUPPORT = 93;    /* Protocol not supported */
    int ESOCKTNOSUPPORT = 94;    /* Socket type not supported */
    int EOPNOTSUPP    =  95;     /* Operation not supported on transport endpoint */
    int EPFNOSUPPORT  =  96;     /* Protocol family not supported */
    int EAFNOSUPPORT  =  97;     /* Address family not supported by protocol */
    int EADDRINUSE    =  98;     /* Address already in use */
    int EADDRNOTAVAIL =  99;     /* Cannot assign requested address */
    int ENETDOWN      = 100;     /* Network is down */
    int ENETUNREACH   = 101;     /* Network is unreachable */
    int ENETRESET     = 102;     /* Network dropped connection because of reset */
    int ECONNABORTED  = 103;     /* Software caused connection abort */
    int ECONNRESET    = 104;     /* Connection reset by peer */
    int ENOBUFS       = 105;     /* No buffer space available */
    int EISCONN       = 106;     /* Transport endpoint is already connected */
    int ENOTCONN      = 107;     /* Transport endpoint is not connected */
    int ESHUTDOWN     = 108;     /* Cannot send after transport endpoint shutdown */
    int ETOOMANYREFS  = 109;     /* Too many references: cannot splice */
    int ETIMEDOUT     = 110;     /* Connection timed out */
    int ECONNREFUSED  = 111;     /* Connection refused */
    int EHOSTDOWN     = 112;     /* Host is down */
    int EHOSTUNREACH  = 113;     /* No route to host */
    int EALREADY      = 114;     /* Operation already in progress */
    int EINPROGRESS   = 115;     /* Operation now in progress */
    int ESTALE        = 116;     /* Stale NFS file handle */
    int EUCLEAN       = 117;     /* Structure needs cleaning */
    int ENOTNAM       = 118;     /* Not a XENIX named type file */
    int ENAVAIL       = 119;     /* No XENIX semaphores available */
    int EISNAM        = 120;     /* Is a named type file */
    int EREMOTEIO     = 121;     /* Remote I/O error */
    int EDQUOT        = 122;     /* Quota exceeded */
    int ENOMEDIUM     = 123;     /* No medium found */
    int EMEDIUMTYPE   = 124;     /* Wrong medium type */
    // @formatter:on

    // signals
    // @formatter:off
    int SIGHUP   = 1;       // Hangup detected on controlling terminal
                            // or death of controlling process
    int SIGINT   = 2;       // Interrupt from keyboard
    int SIGQUIT  = 3;       // Quit from keyboard
    int SIGILL   = 4;       // Illegal Instruction
    int SIGABRT  = 6;       // Abort signal from abort(3)
    int SIGFPE   = 8;       // Floating-point exception
    int SIGKILL  = 9;       // Kill signal
    int SIGSEGV  = 11;      // Invalid memory reference
    int SIGPIPE  = 13;      // Broken pipe: write to pipe with no
                            // readers; see pipe(7)
    int SIGALRM  = 14;      // Timer signal from alarm(2)
    int SIGTERM  = 15;      // Termination signal
    int SIGUSR1  = 10;      // User-defined signal 1
    int SIGUSR2  = 12;      // User-defined signal 2
    int SIGCHLD  = 17;      // Child stopped or terminated
    int SIGCONT  = 18;      // Continue if stopped
    int SIGSTOP  = 19;      // Stop process
    int SIGTSTP  = 20;      // Stop typed at terminal
    int SIGTTIN  = 21;      // Terminal input for background process
    int SIGTTOU  = 22;      // Terminal output for background process
    // @formatter:on

    /**
     * Send a signal to the process with ID pid.
     *
     * @param sig The signal to send.
     * @param pid The process ID of the process that shall receive the signal.
     * @return 0 if successul, error code if an error occured.
     */
    int kill(int pid, int sig);

    /**
     * Close a file descriptor.
     *
     * @param fd The file descriptor to close.
     * @return 0 on success, -1 on error (errno set).
     */
    int close(int fd);
}