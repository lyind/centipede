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

// WebSocket subject (ws) sub-protocol client
(function(broker, rx)
{
    var ws = {};
    
    // reference to the current connection
    ws.socket = undefined;
    
    // find the parent of the last loaded <script> tag
    ws.open = function(url)
    {
        ws.socket = new WebSocket(url);

    };
    
    
    // issue an asynchronous call, returns an observable registered with the broker
    ws.call = function(subject, data)
    {
        if (this.socket)
        {
            
        }
    };
    const socketMessage$ = socket$.flatMap(

    var socket => .share()
    
    
    // publish
    window.ws = ws;
    
}).(broker, rx);