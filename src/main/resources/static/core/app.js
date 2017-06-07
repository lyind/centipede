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
    console.log("[app] init");
    // first, check if we are already loaded
    // start by loading external libraries and then our application
    if (!window.app)
    {
        var appRoot = (function()
        {
          var root = document.scripts[document.scripts.length - 1].src;
          return root.replace(/core\/app\.js$/, "");
        })();

        var requireStore = {};
        var requireStack = [];
        var scheduled = [];
        var isMounted = false;

        var loadViaTag = function(url)
        {
            console.log("[app] load: " + url);
            var script = document.createElement('script');
            script.src = url;

            // notify waiters
            script.onload = function()
            {
                console.log("[app] loaded: " + url);
                var callbacks = requireStore[url];

                var cb = undefined;
                while((cb = callbacks.pop()))
                {
                    cb();
                }
            };

            document.head.appendChild(script);
        };


        var getBasePath = function()
        {
            //return document.location.pathname;
            return appRoot;
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

            return (segments.length === 0) ? "/" : segments.join("/");
        };


        // Test if a value is an object
        var isObject = function(value)
        {
            return value === Object(value);
        };


        // Load scripts at the specified paths and execute callback after all have been loaded.
        var require = function(paths, onComplete)
        {
            paths = (paths.constructor === Array) ? paths : [paths];

            var request = { done: false, "onComplete": onComplete };
            var pathsToLoad = paths.map(canonicalizePath);

            var considerComplete = function(loadedPath)
            {
                for (var i = 0; i < pathsToLoad.length; ++i)
                {
                    if (pathsToLoad[i] === loadedPath)
                    {
                        pathsToLoad[i] = undefined;
                    }
                }

                // nothing else to load? done.
                if (!pathsToLoad.some(function(path) { return (!!path); }))
                {
                    // mark this request as done
                    request.done = true;

                    // finish all completed requests and pop them from the dependency stack
                    for (var i = requireStack.length - 1; i >= 0; --i)
                    {
                        // last request left or already marked as done?
                        if (requireStack[i].done)
                        {
                            var otherRequest = requireStack.pop();
                            if (otherRequest.onComplete)
                            {
                                otherRequest.onComplete();
                            }
                        }
                        else
                        {
                            break;
                        }
                    }
                }
            };

            if (pathsToLoad.length === 0)
            {
                if (onComplete)
                {
                    onComplete();
                }
                return;
            }

            requireStack.push(request);

            pathsToLoad.forEach(function(path)
            {
                var isRequested = Object.prototype.hasOwnProperty.call(requireStore, path);
                if (isRequested && requireStore[path].length == 0)
                {
                    considerComplete(path);
                }
                else
                {
                    var onLoad = function()
                    {
                        considerComplete(path);
                    };

                    if (isRequested)
                    {
                        requireStore[path].push(onLoad);
                    }
                    else
                    {
                        requireStore[path] = [onLoad];
                        loadViaTag(path);
                    }
                }
            });
        };


        // find the parent of the last loaded <script> tag
        var findComponent = function()
        {
            if (document.scripts.length > 0)
            {
                var scriptTag = document.scripts[document.scripts.length - 1];
                if (scriptTag)
                {
                    return scriptTag.parentNode;
                }
            }

            return undefined;
        };


        // schedule a function to run after the app has been mounted
        // the function will be passed the app instance as first argument
        // the DOM element that is parent to the calling <script> element will be passed as second argument
        var schedule = function(fn)
        {
            var root = findComponent();
            var task = fn;
            if (root !== undefined)
            {
                task = function(app) { fn(app, root); };
            }

            if (!isMounted)
            {
                scheduled.push(task);
            }
            else
            {
                task();
            }
        };


        // run scheduled functions
        var runScheduled = function()
        {
            var job = undefined;
            while((job = scheduled.shift()) != null)
            {
                job(this);
            }
        };


        // redirect to actual component, if not at root
        var doRedirect = function()
        {
            var path = window.location.pathname;
            if (path !== "/" && path !== "/index.html")
            {
                window.app.navigate(window.location.pathname + window.location.hash + window.location.search);
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
            Object.defineProperty(app, "isObject", { value: isObject });

            // publish app root object
            Object.defineProperty(window, "app", { value: app });

            // load core components
            require([
                "core/broker.js",
                "core/ui.js",
                "core/centipede-splitter.js",
                "core/ws.js",
                "core/http.js"
            ],
            function()
            {
                console.log("mounting app");

                app.runScheduled();
                isMounted = true;

                console.log("app mounted at: " + getBasePath());

                doRedirect();
            });
        };

        bootstrapApp();
    }

})(window, document);

