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
    "core/broker.js"
],
function()
{
    console.log("[centipede-splitter] init");

    (function(app)
    {
        const SERVICES = "SERVICES";
        const DEPENDENCIES = "DEPENDENCIES";
        const TOKEN = "TOKEN";
        const CREDENTIALS = "CREDENTIALS";
        const ERROR = "ERROR";
        const OVERFLOW = "OVERFLOW";


        var splitter = function(message)
        {
            var parts = [];
            if (message instanceof ArrayBuffer)
            {
                console.log("[centipede-splitter] can't handle message with ArrayBuffer payload");
            }
            else if (message instanceof Blob)
            {
                console.log("[centipede-splitter] can't handle message with Blob payload");
            }
            else
            {
                message = JSON.parse(message);
                if (message.services != null)
                {
                    var serviceIds = [];
                    var serviceCount = message.services.length;

                    // publish list of service IDs
                    serviceIds.length = serviceCount;
                    for (var i = 0; i < serviceCount; ++i)
                    {
                        serviceIds[i] = message.services[i].name;
                    }
                    parts.push([SERVICES, serviceIds]);

                    // publish service details
                    for (var i = 0; i < serviceCount; ++i)
                    {
                        parts.push([[SERVICES, message.services[i].name], message.services[i]]);
                    }
                }
                if (message.dependencies != null)
                {
                    parts.push([DEPENDENCIES, message.dependencies]);
                }
                if (message.token != null)
                {
                    parts.push([TOKEN, message.token]);
                }
                if (message.credentials != null)
                {
                    parts.push([CREDENTIALS, message.credentials]);
                }
                if (message.error != null)
                {
                    parts.push([ERROR, message.error, true]);
                }
                if (message.overflow != null)
                {
                    parts.push([OVERFLOW, true, true]);
                }
            }

            return parts;
        };

        // publish constant subject IDs
        Object.defineProperty(app.subject, SERVICES, { value: SERVICES });
        Object.defineProperty(app.subject, DEPENDENCIES, { value: DEPENDENCIES });
        Object.defineProperty(app.subject, TOKEN, { value: TOKEN });
        Object.defineProperty(app.subject, CREDENTIALS, { value: CREDENTIALS });
        Object.defineProperty(app.subject, ERROR, { value: ERROR });
        Object.defineProperty(app.subject, OVERFLOW, { value: OVERFLOW });

        // publish splitter
        app.splitters.push(splitter);

    })(window.app);
});