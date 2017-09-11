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
app.require([],
    function ()
    {
        console.log("[centipede-service-util] init");
        (function (app)
        {
            var sanitizeService = function (service)
            {
                // skip invalid/self-added/external services
                if (service.kind !== "JAVA" && service.kind !== "NATIVE")
                    return undefined;

                var serviceClone = {};

                serviceClone.name = service.name;
                serviceClone.targetState = service.targetState;
                serviceClone.kind = service.kind;
                serviceClone.vmArguments = service.vmArguments;
                serviceClone.image = service.image;
                serviceClone["arguments"] = service["arguments"];
                serviceClone.route = service.route;
                serviceClone.proxyPathPrefix = service.proxyPathPrefix;

                return serviceClone;
            };

            Object.defineProperty(app, "sanitizeService", {value: sanitizeService});

        })(window.app);
    });