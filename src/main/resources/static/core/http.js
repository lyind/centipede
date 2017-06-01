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

// UI helper
app.require([
    "lib/Rx.js"
],
function()
{
    console.log("[http] init");

    (function(app, document, Rx)
    {
        var getXhrObservable = function(method, url, responseType, headers)
        {
            return Rx.Observable.create(function (observer)
            {
                console.log("[http] GET: " + url);
                var request = new XMLHttpRequest();

                request.open(method, url, true);
                if (responseType)
                {
                    request.responseType = responseType;
                }

                if (app.isObject(headers))
                {
                    for (var name in headers)
                    {
                        if (Object.prototype.hasOwnProperty.call(headers, name))
                        {
                            request.setRequestHeader(name, headers[name]);
                        }
                    }
                }

                request.onreadystatechange = function(e)
                {
                    if (request.readyState === 4)
                    {
                        if (request.status >= 200 && request.status < 300)
                        {
                            observer.next((responseType) ? request.response : request.responseText);
                            observer.complete();
                        }
                        else
                        {
                            var error = new Error(request.statusText);
                            error.status = request.status;

                            throw error;
                        }
                    }
                };

                request.send();

                return function ()
                {
                    if (request.readyState > 1 && request.readyState < 4)
                    {
                        request.abort();
                    }
                };
            });
        };

        var GET = function(url, responseType, headers)
        {
            return getXhrObservable('GET', url, responseType, headers);
        };

        // publish methods
        Object.defineProperty(app, "GET", { value: GET });

    })(window.app, document, window.Rx);

});