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
"use strict";

// Centipede splitter
app.require([
    "core/ws.js",
    "core/broker.js",
    "custom/centipede-splitter.js"
],
function()
{
    console.log("[centipede-joiner] init");

    (function(app, broker)
    {
        var joiner = function(message)
        {
            if (message instanceof ArrayBuffer)
            {
                console.log("[centipede-joiner] can't handle message with ArrayBuffer payload");
            }
            else if (message instanceof Blob)
            {
                console.log("[centipede-joiner] can't handle message with Blob payload");
            }
            else if (typeof message === "string")
            {
                console.log("[centipede-joiner] can't handle message with string payload");
            }
            else
            {
                // add information to messages, here
            }

            return message;
        };

        // publish joiner
        app.joiners.push(joiner);

    })(window.app, window.app.broker);
});