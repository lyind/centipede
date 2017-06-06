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

// WebSocket subject (ws) sub-protocol client
app.require([
    "lib/Rx.js",
    "core/broker.js"
],
function()
{
    console.log("[ws] init");

    (function(app, Rx, broker)
    {
        var ws = {};

        // reference to the current connection
        ws.socket = undefined;

        ws.openId = "ws-open";
        ws.closeId = "ws-close";
        ws.errorId = "ws-error";

        ws.id = 0;

        // open WebSocket connection
        ws.open = function(url)
        {
            ws.socket = new WebSocket(url);

            ++ws.id;

            ws.socket.onopen = function(event)
            {
                console.log("websocket[" + ws.id + "]: opened");
                broker(ws.openId, function() { return Rx.Observable.of(ws.id); });
            };

            ws.socket.onclose = function(event)
            {
                if (event.wasClean)
                {
                    console.log("websocket[" + ws.id + "]: closed");
                    broker(ws.closeId, function() { return Rx.Observable.of(ws.id); });
                }
            };

            ws.socket.onmessage = function(event)
            {
                // use the provided splitter to split the message and feed information to the correct channel
                if (app.splitters)
                {
                    app.splitters.forEach(function(splitter) { splitter(broker, event.data); });
                }
            };

            ws.socket.onerror = function(event)
            {
                console.log("websocket[" + ws.id + "]: error, trying to re-connect");

                broker(ws.errorId, function() { return Rx.Observable.of(ws.id); });

                setTimeout(500, function() { ws.open(url); });
            };
        };


        // issue an asynchronous call, returns an observable registered with the broker
        ws.send = function(data)
        {
            if (ws.socket && ws.socket.readyState === 1)
            {
                ws.socket.send(data);
            }
        };

        // publish
        Object.defineProperty(app, "ws", { value: ws });

    })(window.app, window.Rx, window.app.broker);
});