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
    if (window.app)
    {
        // app already mounted
        return;
    }

    console.log("[app] init");

    const APP_ONCE_ATTRIBUTE_NAME = "data-app-scheduled";

    var bindById = function(object, parentNode)
    {
        Object.defineProperty(object, "parent", { value: parentNode});
        if (object.parent)
        {
            Array.prototype.forEach.call(object.parent.querySelectorAll('*[id]:not([id=""])'), function(e)
            {
                Object.defineProperty(object, e.id, { value: e });
            });
        }

        return object;
    };

    // Publish app main function.
    // Calling it schedules a function to run after the app has been mounted.
    // The functions "this" will be pointing to the app instance.
    // The DOM element that is parent to the calling <script> element will be passed as second argument
    Object.defineProperty(window, "app", { value: function(fn, onlyOnce)
    {
        var script = document.currentScript;

        if (script && onlyOnce)
        {
            if (script.hasAttribute(APP_ONCE_ATTRIBUTE_NAME))
            {
                return;
            }

            script.setAttribute(APP_ONCE_ATTRIBUTE_NAME, "");
        }

        // bind all local elements by id
        fn = bindById(fn, (script && script.parentNode) ? script.parentNode : undefined);

        if (!isReady) // schedule during initial load or navigate (app already mounted)
        {
            scheduled.push(fn);
        }
        else
        {
            fn(fn, window.app);
        }
    }});
    var app = window.app;

    // Same as app() but executes only once (does nothing on dynamic script re-execution).
    Object.defineProperty(app, "once", { value: function(fn) { return app(fn, true); } });

    // Take up all nodes below parent into the object (named by their ID attribute values).
    Object.defineProperty(app, "bindById", { value: function(object, parentNode) { return bindById(object, parentNode); } });

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
    var isReady = 0; // 0 - neither app loaded nor window.onload called, 1 - app loaded or window.onload, 2 - both


    var loadViaTag = function(url)
    {
        console.log("[app] load: " + url);
        var script = document.createElement('script');
        script.src = url;

        // notify waiters
        script.onload = function()
        {
            var subRequests = requireStore[url];
            while(subRequests.length > 0)
            {
                subRequests.pop()();
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


    var requireFinishCompleted = function()
    {
        // finish all completed requests and pop them from the dependency stack
        for (var i = requireStack.length - 1; i >= 0 && requireStack[i].onComplete !== undefined; --i)
        {
            // last request left or already marked as done?
            requireStack.pop().onComplete();
        }
    };


    var requireMarkPathLoaded = function(pathsToLoad, loadedIndex)
    {
        pathsToLoad[loadedIndex] = undefined;
        var iUnloaded = 0;
        var pathsLength = pathsToLoad.length;
        while(iUnloaded < pathsLength && pathsToLoad[iUnloaded] === undefined)
        {
            ++iUnloaded;
        }

        return (iUnloaded === pathsLength);
    };


    var requirePushRequest = function(pathsToLoad, request)
    {
        // push new request on stack and make sure callbacks of dependencies get called first
        var stackLength = requireStack.length;
        for (var i = 0; i < stackLength; ++i)
        {
            if (pathsToLoad.indexOf(requireStack[i].requester.pathname) >= 0)
            {
                // insert this request below all possible dependencies
                for (var j = stackLength; j > i; --j)
                {
                    requireStack[j] = requireStack[j - 1];
                }
                requireStack[i] = request;
                return;
            }
        }

        requireStack.push(request);
    };


    var canonicalizeAllPaths = function(paths)
    {
        var canonicalPaths = [];
        canonicalPaths.length = paths.length;
        for (var i = 0; i < paths.length; ++i)
        {
            canonicalPaths[i] = canonicalizePath(paths[i]);
        }
        return canonicalPaths;
    };


    // Load scripts at the specified paths and execute callback after all have been loaded.
    var require = function(paths, onComplete)
    {
        paths = (paths.constructor === Array) ? paths : ((paths) ? [paths] : []);
        if (!onComplete)
        {
            onComplete = function() {};
        }

        var pathsToLoad = canonicalizeAllPaths(paths);
        if (pathsToLoad.length === 0)
        {
            onComplete();
            return;
        }

        var requester = (document.currentScript) ? new URL(document.currentScript.src) : "<anonymous>";
        var request = { onComplete: undefined, requester: requester };

        // push new request on stack and make sure callbacks of all scripts required by this request get called first
        requirePushRequest(pathsToLoad, request);

        var requireConsiderComplete = function(loadedIndex)
        {
            if (requireMarkPathLoaded(pathsToLoad, loadedIndex))
            {
                // mark this request as done
                request.onComplete = onComplete;
            }

            requireFinishCompleted();
        };

        for (var i = 0; i < pathsToLoad.length; ++i)
        {
            var path = pathsToLoad[i];
            var isRequested = Object.prototype.hasOwnProperty.call(requireStore, path);
            if (isRequested && requireStore[path].length === 0)
            {
                requireConsiderComplete(i);
            }
            else
            {
                var onLoad = (function(loadedIndex)
                {
                    return function()
                    {
                        requireConsiderComplete(loadedIndex);
                    };
                })(i);

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
        }
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
        isReady = true;
        for (var job = scheduled.shift(); job; job = scheduled.shift())
        {
            job(job, app);
        }
    };


    var resetIsReady = function()
    {
        isReady = false;
    };


    // perform certain default actions, depending on the type of element
    var handle = function(event)
    {
        var handler = undefined;
        if (event instanceof Event)
        {
            var target = event.target;
            if (event.type === "click" && target && (target.nodeName === "a" || target.nodeName === "A"))
            {
                handler = function(that, app)
                {
                    // that from document.currentScript won't ever be available in the context of an event handler
                    app.navigate(target.pathname + target.hash + target.search);
                }
            }
            else
            {
                console.log("[app] no default action for: ", event);
            }
        }
        else
        {
            console.log("[app] handle() can only handle events, not this: ", event);
        }

        if (handler !== undefined)
        {
            event.stopPropagation();
            event.preventDefault();

            app(handler);
        }
    };


    // redirect to actual component, if not at root
    var setupInitialRoute = function()
    {
        // clear jobs queued already, these can't do any good
        scheduled.length = 0;

        // ensure initial navigation (user entering application/coming back using deep link)
        var loc = window.location;
        app.navigate(loc.pathname + loc.hash + loc.search);
    };


    // load application parts

    // publish app API methods
    Object.defineProperty(app, "canonicalizePath", { value: canonicalizePath });
    Object.defineProperty(app, "require", { value: require });
    Object.defineProperty(app, "requireAllLoaded", { value: requireAllLoaded });
    Object.defineProperty(app, "resetIsReady", { value: resetIsReady });
    Object.defineProperty(app, "handle", { value: handle });

    // load core components
    require([
        "core/broker.js",
        "core/ui.js",
        "core/ws.js",
        "core/http.js",
        "core/util.js",
        "polyfill/polyfill.js", // load all registered polyfills
        "custom/custom.js"  // all app-specific custom code
    ],
    function()
    {
        console.log("[app] mounted at: " + app.baseUrl.pathname);

        app.ws.open(app.baseUrl.href.replace("http", "ws"));

        window.addEventListener("load", considerRunningScheduled);
        setupInitialRoute();
    });

})(window, document);

