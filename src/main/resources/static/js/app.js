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
(function(window, document)
{
    // first, check if we are already loaded
    // start by loading external libraries and then our application
    if (!window.app)
    {
        var requireStore = {};
        var scheduled = [];
        var isMounted = false;

        var loadViaTag = function(url, onLoad)
        {
            var script = document.createElement('script');
            script.src = url;
            script.onload = onLoad;
            document.head.appendChild(script);
        };


        var getBasePath = function()
        {
            return document.location.pathname;
        }


        var canonicalizePath = function(relativePath)
        {
            var relativeSegments = relativePath.split("/");

            // handle absolute paths
            var basePath = "";
            if (relativeSegments.length <= 0 || relativeSegments[0] !== "")
            {
                basePath = getBasePath();
            }

            var segments = basePath.split("/");
            if (segments.length > 1)
            {
                segments.pop();
            }

            for (var i = 0; i < relativeSegments.length; ++i)
            {
                var segment = relativeSegments[i];
                if (segment !== ".")
                {
                    if (segment === "..")
                    {
                        segments.pop();
                    }
                    else
                    {
                        segments.push(segment);
                    }
                }
            }

            return segments.join("/");
        };


        // Load scripts at the specified URLs and execute callback after all have been loaded.
        var require = function(urls, onComplete)
        {
            urls = (urls.constructor === Array) ? urls : [urls];

            var urlsToLoad = urls.map(canonicalizePath);

            var considerComplete = function(url, isJustLoaded)
            {
                if (isJustLoaded)
                {
                    requireStore[url] = true;
                }

                for (var i = 0; i < urlsToLoad.length; ++i)
                {
                    if (urlsToLoad[i] === url)
                    {
                        urlsToLoad[i] = undefined;
                    }
                }

                // nothing else to load? done.
                if (!urlsToLoad.some(function(url) { return url !== undefined; }))
                {
                    onComplete();
                }
            };

            urlsToLoad.forEach(function(url)
            {
                if (url != null)
                {
                    if (Object.prototype.hasOwnProperty.call(requireStore, url))
                    {
                        considerComplete(url, false);
                    }
                    else
                    {
                        requireStore[url] = false;
                        loadViaTag(url, function() { considerComplete(url, true); });
                    }
                }
            });
        };

        // schedule a function to run after the app has been mounted
        // the function will be passed the app instance as first argument
        var schedule = function(fn)
        {
            if (!isMounted)
            {
                scheduled.push(fn);
            }
            else
            {
                fn();
            }
        };


        // run scheduled functions
        var runScheduled = function()
        {
            var job = undefined;
            while((job = scheduled.shift()) != null)
            {
                job(window.app);
            }
        };


        // load application parts
        var bootstrapApp = function()
        {
            var app = {};

            // publish app API methods
            Object.defineProperty(app, "getBasePath", { value: getBasePath });
            Object.defineProperty(app, "canonicalizePath", { value: canonicalizePath });
            Object.defineProperty(app, "require", { value: require });
            Object.defineProperty(app, "schedule", { value: schedule });
            Object.defineProperty(app, "runScheduled", { value: runScheduled });

            // publish app root object
            Object.defineProperty(window, "app", { value: app });

            // load core components
            require([
                "js/broker.js",
                "js/ui.js",
                "js/centipede-splitter.js",
                "js/ws.js"
            ],
            function()
            {
                console.log("mounting app");

                app.runScheduled();
                isMounted = true;

                console.log("app mounted at: " + getBasePath());
            });
        };

        bootstrapApp();
    }

})(window, document);

