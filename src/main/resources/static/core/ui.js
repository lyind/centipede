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
        "lib/Rx.js",
        "core/broker.js",
        "core/http.js",
        "polyfill/polyfill.js"
    ],
    function ()
    {
        console.log("[ui] init");

        (function (app, document, Rx, broker, brokerEvent)
        {
            const LEAVE_ROUTE = "LEAVE_ROUTE";
            const NEXT_ROUTE = "NEXT_ROUTE";
            const ROUTE = "ROUTE";
            const ATTRIBUTE_KEEP = "data-keep";

            var html5doctype = document.implementation.createDocumentType('html', '', '');
            var keepStore = document.implementation.createDocument('', 'html', html5doctype);
            var keepables = {};
            var encounteredScriptUrls = {};

            // ensure these subjects are kept available
            brokerEvent(LEAVE_ROUTE).subscribe();
            brokerEvent(NEXT_ROUTE).subscribe();
            broker(ROUTE).subscribe();

            // request a full DOM document
            var getDocument = function (canonicalPath)
            {
                return app.GET(canonicalPath, "document", {});
            };

            // request a DOM document and return it's body as a document fragment of the current document
            var getFragment = function (targetDocument, canonicalPath)
            {
                return getDocument(canonicalPath).map(function (doc)
                {
                    var fragment = targetDocument.createDocumentFragment();
                    var children = doc.body.childNodes;
                    var length = children.length;
                    for (var i = 0; i < length; ++i)
                    {
                        var fragmentNode = targetDocument.importNode(children[i], true);
                        fragment.appendChild(fragmentNode);
                    }

                    return fragment;
                });
            };

            var forceScriptExecution = function (targetDocument, scriptNodes)
            {
                var length = scriptNodes.length;
                for (var i = 0; i < length; ++i)
                {
                    var script = scriptNodes[i];
                    var parent = script.parentNode;

                    var newScript = targetDocument.createElement("script");

                    // copy attributes
                    for (var j = 0; j < script.attributes.length; ++j)
                    {
                        var attribute = script.attributes[j];
                        newScript.setAttribute(attribute.name, attribute.value);
                    }

                    newScript.appendChild(targetDocument.createTextNode(script.innerHTML));

                    parent.replaceChild(newScript, script);
                }
            };

            var initializeKeepable = function (targetDocument, keepableNode, keepableSource)
            {
                if (keepableSource)
                {
                    if (keepableSource.endsWith(".js"))
                    {
                        // script only fragment requested
                        app.require(keepableSource, function ()
                        {
                            keepables[keepableSource] = keepableNode;
                        });
                    }
                    else
                    {
                        // HTML fragment requested
                        getFragment(targetDocument, app.canonicalizePath(keepableSource)).subscribe(function (fragment)
                        {
                            keepableNode.appendChild(fragment);
                            forceScriptExecution(targetDocument, Array.prototype.map.call(keepableNode.getElementsByTagName("script"), function (node)
                            {
                                return node;
                            }));

                            keepables[keepableSource] = keepableNode;
                        });
                    }
                }
            };

            var initializeNewKeepables = function (targetDocument, newKeepables)
            {
                for (var src in newKeepables)
                {
                    if (Object.prototype.hasOwnProperty.call(newKeepables, src))
                    {
                        initializeKeepable(targetDocument, newKeepables[src], src);
                    }
                }
            };

            var placeKeepAnnotated = function (sourceDocument, targetDocument)
            {
                var targetKeepables = {};
                Array.prototype.forEach.call(targetDocument.querySelectorAll('[' + ATTRIBUTE_KEEP + ']'),
                    function(keepableNode)
                    {
                        var src = keepableNode.getAttribute(ATTRIBUTE_KEEP);
                        if (src)
                        {
                            targetKeepables[src] = keepableNode;
                        }
                    });

                // put all nodes back where they belong
                // in case someone doesn't like them in the new document we still keep them around in our store
                for (var src in keepables)
                {
                    if (!Object.prototype.hasOwnProperty.call(keepables, src))
                        continue;

                    var keepable = keepables[src];
                    if (Object.prototype.hasOwnProperty.call(targetKeepables, src))
                    {
                        var template = targetKeepables[src];

                        // we know this keepable is initialized
                        delete targetKeepables[src];

                        // replace template with the "data-keep" annotated thing
                        template.parentNode.replaceChild((keepables[src] = targetDocument.importNode(keepable, true)), template);

                        if (keepable.ownerDocument === keepStore)
                            keepable.parentNode.removeChild(keepable);

                        console.log("[ui] keepable restored: " + src);
                    }
                    else if (keepable.ownerDocument !== keepStore)
                    {
                        // stash in store
                        keepables[src] = keepStore.documentElement.appendChild(keepStore.importNode(keepable, true));
                    }
                    else
                    {
                        // keep stashed away in store
                        continue;
                    }
                }

                initializeNewKeepables(targetDocument, targetKeepables);
            };

            var sliceAtIndexOf = function (subject, separator)
            {
                var i = subject.indexOf(separator);
                if (i >= 0)
                {
                    return [subject.slice(0, i), subject.slice(i)];
                }
                return [subject, ""];
            };

            var splitPath = function (pathHashAndQuery)
            {
                var url = {path: "", hash: "", query: ""};
                var parts = [pathHashAndQuery, ""];

                parts = sliceAtIndexOf(parts[0], '?');
                url.query = parts[1];

                parts = sliceAtIndexOf(parts[0], '#');
                url.hash = parts[1];
                url.path = parts[0];

                return url;
            };

            var splitRoute = function (canonicalPathWithArgs)
            {
                // TODO Use Object.defineProperty here.
                var route = {
                    routes: [],
                    strippedPath: "",
                    subscription: undefined,
                    canonicalPathWithArgs: canonicalPathWithArgs,
                    current: undefined
                };

                var path = [];
                var segments = canonicalPathWithArgs.split("/");
                for (var i = 0; i < segments.length;)
                {
                    path.push(segments[i]);

                    var args = [];
                    for (i = i + 1; i < segments.length; ++i)
                    {
                        if (!segments[i].startsWith(":"))
                            break;

                        args.push(segments[i].slice(1));
                    }

                    route.routes.push({"path": path.join("/"), "args": args});
                }

                route.strippedPath = path.join("/");
                route.current = route.routes[route.routes.length - 1];

                return route;
            };

            var navigateOnPopState = function (e)
            {
                // navigate to the "current" page
                var loc = window.location;
                app.navigate(loc.pathname + loc.hash + loc.search);
            };

            var navigateRelative = function (relativePath)
            {
                var parts = splitPath(relativePath);
                var canonicalPathWithArgs = app.canonicalizePath(parts.path);
                var route = splitRoute(canonicalPathWithArgs);

                // copy reference to avoid some firefox bug
                var currentDocument = document;
                route.subscription = getDocument(route.strippedPath).subscribe(function (newDocument)
                    {
                        try
                        {
                            // notify subscribers about next route (subscribers may cancel the routing attempt using "subscription"
                            brokerEvent(app.subject.LEAVE_ROUTE, function ()
                            {
                                return Rx.Observable.of(route);
                            }).pull();

                            app.resetIsReady();

                            // place persistent elements with "keep=PARENT_ID" attribute
                            placeKeepAnnotated(currentDocument, newDocument);

                            // keep script urls to allow for de-duplication to avoid reloading scripts already present
                            var livePreviousScripts = currentDocument.getElementsByTagName("SCRIPT");
                            for (var i = 0; i < livePreviousScripts.length; ++i)
                            {
                                encounteredScriptUrls[livePreviousScripts[i].src] = null;
                            }

                            // publish argument updates for all parent routes and this route
                            for (var i = 0; i < route.routes.length; ++i)
                            {
                                broker([ROUTE, route.routes[i].path], Rx.Observable.of(route.routes[i].args)).pull();
                            }

                            // notify subscribers about changed route
                            broker(ROUTE, function ()
                            {
                                return Rx.Observable.of(route);
                            }).pull();

                            // perform document replace
                            currentDocument.replaceChild(newDocument.documentElement, currentDocument.documentElement);

                            // fix links in IE
                            if (currentDocument.createStyleSheet)
                            {
                                var links = currentDocument.getElementsByTagName("LINK");
                                for (var i = 0; i < links.length; ++i)
                                {
                                    links[i].href = links[i].href;
                                }
                            }

                            // enforce script execution
                            forceScriptExecution(currentDocument, Array.prototype.filter.call(currentDocument.getElementsByTagName("script"), function (script)
                            {
                                if (script.parentNode.nodeName === "HEAD")
                                {
                                    if (Object.prototype.hasOwnProperty.call(encounteredScriptUrls, script.src))
                                    {
                                        //console.log("[ui] skip: ", script.src);
                                        // skip this script
                                        return false;
                                    }
                                }
                                return true;
                            }));
                        }
                        catch (e)
                        {
                            console.error("[ui] failed to construct target document: " + route.strippedPath + ": ", e);
                            currentDocument = currentDocument.open("text/html");
                            currentDocument.write(newDocument.documentElement.outerHTML);
                            currentDocument.close();
                        }

                        // change path to reflect actual location
                        var title = "";
                        var titleNode = currentDocument.getElementsByTagName("TITLE");
                        if (titleNode.length > 0 && titleNode[0].childNodes.length > 0)
                        {
                            title = titleNode[0].childNodes[0].nodeValue;
                        }

                        var targetPath = canonicalPathWithArgs + parts.hash + parts.query;
                        window.history.pushState(undefined, title, targetPath);

                        window.addEventListener("popstate", navigateOnPopState);

                        app.requireAllLoaded(function ()
                        {
                            window.dispatchEvent(new Event("load"));
                        });

                        console.log("[ui] navigated to: " + targetPath + ", title: " + title);
                    },
                    function (e)
                    {
                        console.error("[ui] failed to navigate to: " + route.strippedPath + ": ", e);
                    });

                // notify subscribers about next route (subscribers may cancel the routing attempt using "subscription"
                brokerEvent(NEXT_ROUTE, function ()
                {
                    return Rx.Observable.of(route);
                }).pull();

                console.log("[ui] navigate scheduled");
            };

            Object.defineProperty(app, "navigate", {
                value: function (relativeDestination)
                {
                    if (relativeDestination === undefined || relativeDestination === null || relativeDestination === ".")
                    {
                        return brokerEvent(LEAVE_ROUTE);
                    }

                    var dst = relativeDestination;
                    if (dst instanceof Object && dst.originalEvent)
                    {
                        dst.stopPropagation();
                        dst.preventDefault();
                        dst = dst.originalEvent;
                    }

                    if (dst instanceof Event)
                        dst = dst.target;

                    if (dst instanceof Element)
                        dst = dst.getAttribute("href");

                    navigateRelative(dst);
                }
            });

            // returns an Observable that receives the current route fragment, including arguments
            Object.defineProperty(app, "activeRoute", {
                value: broker(ROUTE)
                    .map(function (route) { return route.current; })
                    .takeUntil(app.navigate())
            });

            // publish constants
            Object.defineProperty(app.subject, LEAVE_ROUTE, {value: LEAVE_ROUTE});
            Object.defineProperty(app.subject, NEXT_ROUTE, {value: NEXT_ROUTE});
            Object.defineProperty(app.subject, ROUTE, {value: ROUTE});

        })(window.app, document, window.Rx, window.app.broker, window.app.brokerEvent);

    });