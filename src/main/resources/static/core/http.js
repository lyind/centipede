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
    function ()
    {
        console.log("[http] init");

        (function (app, document, Rx)
        {
            var GET = function (url, responseType, headers)
            {
                return Rx.Observable.ajax({
                    method: 'GET',
                    url: url,
                    responseType: responseType,
                    async: true,
                    headers: headers
                })
                    .map(function (ajaxResponse)
                    {
                        return ajaxResponse.response;
                    });
            };

            var POST = function (url, responseType, headers, body, contentType)
            {
                var mergedHeaders = headers || {};
                if (contentType)
                {
                    mergedHeaders["Content-Type"] = contentType;
                }

                return Rx.Observable.ajax({
                    method: 'POST',
                    url: url,
                    body: body,
                    responseType: responseType,
                    async: true,
                    headers: mergedHeaders
                })
                    .map(function (ajaxResponse)
                    {
                        return ajaxResponse.response;
                    });
            };

            // publish methods
            Object.defineProperty(app, "GET", {value: GET});
            Object.defineProperty(app, "POST", {value: POST});

        })(window.app, document, window.Rx);

    });