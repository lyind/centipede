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
        // publish app main function
        // calling it schedules a function to run after the app has been mounted
        // the functions "this" will be pointing to the app instance
        // the DOM element that is parent to the calling <script> element will be passed as second argument
        Object.defineProperty(window, "app", { value: function(fn)
        {
            var script = document.currentScript;

            // bind ourselves to the callback
            Object.defineProperty(fn, "app", { value: this });

            // bind all local elements by id
            Object.defineProperty(fn, "parent", { value: (script && script.parentNode) ? script.parentNode : undefined});
            if (fn.parent)
            {
                Array.prototype.forEach.call(fn.parent.querySelectorAll('*[id]:not([id=""])'), function(e)
                {
                    Object.defineProperty(fn, e.id, { value: e });
                });
            }

            if (readyState < 2)
            {
                scheduled.push(fn);
            }
            else
            {
                fn(fn, window.app);
            }
        }});
        var app = window.app;

        // set app root URL to the parent of this scripts directory
        Object.defineProperty(app, "baseUrl", { value:
            (function()
            {
              var src = document.currentScript.src;
              return new URL(src.slice(0, src.lastIndexOf("/", src.lastIndexOf("/") - 1) + 1));
            })()
        });

        // internally used variables
        var requireStore = {};
        var requireStack = [];
        var scheduled = [];
        var readyState = 0; // 0 - neither app loaded nor window.onload called, 1 - app loaded or window.onload, 2 - both


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


        var canonicalizePath = function(relativePath)
        {
            var relativeSegments = relativePath.replace("//", "/").split("/");

            // handle absolute paths
            var basePath = "";
            if (relativeSegments.length <= 0 || relativeSegments[0] !== "")
            {
                basePath = app.baseUrl.pathname;
            }
            else
            {
                // absolute path specified, cut away first empty string
                relativeSegments.shift();
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
                        // special handling of positional arguments (start with ":"): pop all
                        do
                        {
                            segments.pop();
                        }
                        while(segments.length > 0 && segments[segments.length - 1].startsWith(":"));
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
            paths = (paths.constructor === Array) ? paths : ((paths) ? [paths] : []);

            var requester = (document.currentScript) ? new URL(document.currentScript.src) : "<anonymous>";
            var request = { done: false, "onComplete": onComplete, "requester": requester };
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

            // push new request on stack and make sure callbacks of all scripts required by this request get called first
            requireStack.push(request);
            var stackLength = requireStack.length;
            for (var i = stackLength - 2; i >= 0; --i)
            {
                var frame = requireStack[i];
                if (pathsToLoad.indexOf(frame.requester.pathname) >= 0)
                {
                    // put this frame on top of the stack, shift all higher frames down
                    for (var j = i + 1; j < stackLength; ++j)
                    {
                        requireStack[j - 1] = requireStack[j];
                    }
                    requireStack[j - 1] = frame;
                }
            }

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


        // call onComplete as soon as require did load all currently queued scripts
        var requireAllLoaded = function(onComplete)
        {
            var allScripts = [];
            for (var path in requireStore)
            {
                if (Object.prototype.hasOwnProperty.call(requireStore, path))
                {
                    allScripts.push(path);
                }
            }

            require(allScripts, onComplete);
        };


        // run scheduled functions on the second call
        var considerRunningScheduled = function()
        {
            ++readyState;
            if (readyState >= 2)
            {
                var job = undefined;
                while((job = scheduled.shift()) != null)
                {
                    job(job, app);
                }
            }
        };


        var resetReadyState = function()
        {
            if (readyState > 1)
            {
                readyState = 1;
            }
        };


        // redirect to actual component, if not at root
        var doRedirect = function()
        {
            // ensure initial navigation (user entering application/coming back using deep link)
            var loc = window.location;
            app.navigate(loc.pathname + loc.hash + loc.search);

            considerRunningScheduled();
        };


        // load application parts
        var bootstrapApp = function()
        {
            // publish app API methods
            Object.defineProperty(app, "canonicalizePath", { value: canonicalizePath });
            Object.defineProperty(app, "require", { value: require });
            Object.defineProperty(app, "requireAllLoaded", { value: requireAllLoaded });
            Object.defineProperty(app, "resetReadyState", { value: resetReadyState });
            Object.defineProperty(app, "isObject", { value: isObject });

            // load core components
            require([
                "core/broker.js",
                "core/ui.js",
                "core/centipede-splitter.js",
                "core/ws.js",
                "core/http.js",
                "core/util.js"
            ],
            function()
            {
                console.log("[app] mounted at: " + app.baseUrl.pathname);

                app.ws.open(app.baseUrl.href.replace("http", "ws"));

                doRedirect();
            });
        };

        window.addEventListener("load", considerRunningScheduled);
        bootstrapApp();
    }

})(window, document);

