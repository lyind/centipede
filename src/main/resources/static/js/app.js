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

// Application entry point
(function(window)
{
    // only load if not loaded already (for example after a document switch)
    if (!window.app)
    {
        var loadViaTag = function(url, onLoad)
        {
            var script = document.createElement('script');
            script.src = url;
            script.onload = onLoad;
            document.head.appendChild(script);
        };


        // load an array of scripts from the specified urls and execute callback when all are loaded
        var require = function(urls, onComplete)
        {
            var urlsToLoad = (urls.constructor === Array) ? urls.slice() : [urls];

            urls.forEach(function(url)
            {
                loadViaTag(url, function()
                {
                    var i = urlsToLoad.indexOf(url);
                    if (i > -1)
                    {
                        urlsToLoad.splice(i, 1);
                    }

                    if (urlsToLoad.length === 0)
                    {
                        onComplete();
                    }
                });
            });
        };


        // load application parts
        var bootstrapApp = function()
        {
            var app = {};

            // publish app root object
            Object.defineProperty(app, "require", { value: require });

            // publish require method
            Object.defineProperty(window, "app", { value: app });

            require([
                "js/broker.js",
                "js/cell.js",
                "js/centipede-splitter.js",
                "js/ws.js"
            ],
            function() {console.log("app ready")});
        };

        // first, check if we are already loaded
        // start by loading external libraries and then our application
        require([
            "js/Rx.js"
        ],
        bootstrapApp);
    }

})(window);

