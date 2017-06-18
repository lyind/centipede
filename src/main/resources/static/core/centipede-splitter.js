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
    "lib/Rx.js",
    "core/broker.js"
],
function()
{
    console.log("[centipede-splitter] init");

    (function(app, broker, Rx)
    {
        const SERVICES = "SERVICES";
        const TOKEN = "TOKEN";
        const CREDENTIALS = "CREDENTIALS";
        const ERROR = "ERROR";
        const OVERFLOW = "OVERFLOW";

        var splitter = function(broker, message)
        {
            if (message.services != null)
            {
                broker(SERVICES, function() { return Rx.Observable.of(message.services); });
            }
            if (message.token != null)
            {
                broker(TOKEN, function() { return Rx.Observable.of(message.token); });
            }
            if (message.credentials != null)
            {
                broker(CREDENTIALS, function() { return Rx.Observable.of(message.credentials); });
            }
            if (message.error != null)
            {
                broker(ERROR, function() { return Rx.Observable.of(message.error); });
            }
            if (message.overflow != null)
            {
                broker(OVERFLOW, function() { return Rx.Observable.of(true); });
            }
        };

        // publish constant subject IDs
        Object.defineProperty(app.subject, SERVICES, { value: SERVICES });
        Object.defineProperty(app.subject, TOKEN, { value: TOKEN });
        Object.defineProperty(app.subject, CREDENTIALS, { value: CREDENTIALS });
        Object.defineProperty(app.subject, ERROR, { value: ERROR });
        Object.defineProperty(app.subject, OVERFLOW, { value: OVERFLOW });

        // publish splitter
        app.splitters.push(splitter);

    })(window.app, window.app.broker, window.Rx);
});