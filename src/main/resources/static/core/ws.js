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
    "core/util.js",
    "core/broker.js"
],
function()
{
    console.log("[ws] init");

    (function(app, Rx, broker)
    {
        const WEBSOCKET_OPEN = "WEBSOCKET_OPEN";
        const WEBSOCKET_CLOSE = "WEBSOCKET_CLOSE";
        const WEBSOCKET_ERROR = "WEBSOCKET_ERROR";

        // keep these subjects available
        broker(WEBSOCKET_OPEN).subscribe();
        broker(WEBSOCKET_CLOSE).subscribe();
        broker(WEBSOCKET_ERROR).subscribe();

        // publish main ws function
        // issues an asynchronous call, returns an observable registered with the broker
        Object.defineProperty(app, "ws", { value: function(data)
        {
            if (app.ws.socket && app.ws.socket.readyState === 1)
            {
                // use the provided joiners to convert/enhance the message
                var joiners = app.joiners;
                if (joiners)
                {
                    for (var i = 0; i < joiners.length; ++i)
                    {
                        data = joiners[i](data);
                    }
                }

                if (app.isObject(data))
                {
                    data = JSON.stringify(data);
                }

                app.ws.socket.send(data);
            }
        }});
        var ws = app.ws;

        // reference to the current connection
        Object.defineProperty(ws, "socket", { writable: true});
        Object.defineProperty(ws, "id", { value: 0, writable: true});


        // open WebSocket connection
        ws.open = function(url)
        {
            ws.socket = new WebSocket(url);

            ++ws.id;

            ws.socket.onopen = function(event)
            {
                console.log("[ws][" + ws.id + "] socket opened");
                broker(WEBSOCKET_OPEN, function() { return Rx.Observable.of(ws.id); }).pull();
            };

            ws.socket.onclose = function(event)
            {
                if (event.wasClean)
                {
                    console.log("[ws][" + ws.id + "] closed");
                    broker(WEBSOCKET_CLOSE, function() { return Rx.Observable.of(ws.id); }).pull();
                }
            };

            ws.socket.onmessage = function(event)
            {
                // use the provided splitter to split the message and feed information to the correct channel
                var splitters = app.splitters;
                if (splitters)
                {
                    for (var i = 0; i < splitters.length; ++i)
                    {
                        var parts = splitters[i](event.data);
                        for (var j = 0; j < parts.length; ++j)
                        {
                            if (parts[j].length > 1)
                            {
                                // inject event or regular cached value
                                broker(parts[j][0], parts[j][1], parts[j][2]).pull();
                            }
                            else
                            {
                                console.error("[ws] splitter returned invalid parts: ", parts[j]);
                            }
                        }
                    }
                }
            };

            ws.socket.onerror = function(event)
            {
                console.log("[ws][" + ws.id + "] error, trying to re-connect");

                broker(WEBSOCKET_ERROR, function() { return Rx.Observable.of(ws.id); }).pull();

                setTimeout(500, function() { ws.open(url); });
            };
        };

        // status subject
        Object.defineProperty(app.subject, WEBSOCKET_OPEN, { value: WEBSOCKET_OPEN});
        Object.defineProperty(app.subject, WEBSOCKET_CLOSE, { value: WEBSOCKET_CLOSE});
        Object.defineProperty(app.subject, WEBSOCKET_ERROR, { value: WEBSOCKET_ERROR});

        // sub-protocol joiner and splitter registry (like input/output filters)
        Object.defineProperty(app, "splitters", { value: [] });
        Object.defineProperty(app, "joiners", { value: [] });

    })(window.app, window.Rx, window.app.broker);
});