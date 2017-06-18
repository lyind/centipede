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
    "core/http.js"
],
function()
{
    console.log("[ui] init");

    (function(app, document, Rx, broker)
    {
        const ROUTE = "route";

        var html5doctype = document.implementation.createDocumentType( 'html', '', '');
        var keepStore = document.implementation.createDocument('', 'html', html5doctype);
        var encounteredScriptUrls = {};


        // request a full DOM document
        var getDocument = function(canonicalPath)
        {
            return app.GET(canonicalPath, "document", {});
        };


        // request a DOM document and return it's body as a document fragment of the current document
        var getFragment = function(targetDocument, canonicalPath)
        {
            return getDocument(canonicalPath).map(function(doc)
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


        var forceScriptExecution = function(targetDocument, scriptNodes)
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


        var placeKeepAnnotated = function(sourceDocument, targetDocument)
        {
            var nodesToKeep = [];
            var nodesToKeepFromCurrent = sourceDocument.querySelectorAll("[data-keep]");
            for (var i = 0; i < nodesToKeepFromCurrent.length; ++i)
            {
                nodesToKeep.push(nodesToKeepFromCurrent[i]);
            }
            var nodesToKeepFromStore = keepStore.querySelectorAll("[data-keep]");
            for (var i = 0; i < nodesToKeepFromStore.length; ++i)
            {
                nodesToKeep.push(nodesToKeepFromStore[i]);
            }

            // put all nodes back where they belong
            // in case someone doesn't like them in the new document they are still there but not linked
            for (var i = 0; i < nodesToKeep.length; ++i)
            {
                var sourceNode = nodesToKeep[i];
                var sourceNodeParent = sourceNode.parentNode;
                var id = sourceNode.attributes.id.value;
                if (id)
                {
                    var template = targetDocument.getElementById(id);
                    if (template)
                    {
                        // replace template with the "keep" annotated thing
                        var node = targetDocument.importNode(sourceNode, true);
                        template.parentNode.replaceChild(node, template);
                        sourceNodeParent.removeChild(sourceNode);
                    }
                    else
                    {
                        if (sourceNode.ownerDocument === keepStore)
                        {
                            // nothing to do
                            continue;
                        }

                        // keep stashed away in store
                        var node = keepStore.importNode(sourceNode, true);
                        keepStore.documentElement.appendChild(node);
                        sourceNodeParent.removeChild(sourceNode);
                    }

                    console.log("[ui] keepable restored: " + id);
                }
                else
                {
                    console.error("[ui] element with attr. data-keep but no id: ", sourceNode.nodeName);
                }
            }

            var newKeepables = targetDocument.querySelectorAll("[data-keep]");
            for (var i = 0; i < newKeepables.length; ++i)
            {
                var keepableNode = newKeepables[i];
                var src = keepableNode.getAttribute("data-keep");
                if (src)
                {
                    app.schedule(function()
                    {
                        if (src.endsWith(".js"))
                        {
                            // script only fragment requested
                            app.require(src, function() { keepableNode.setAttribute("data-keep", ""); });
                        }
                        else
                        {
                            // HTML fragment requested
                            getFragment(targetDocument, app.canonicalizePath(src)).subscribe(function(fragment)
                            {
                                keepableNode.appendChild(fragment);
                                forceScriptExecution(targetDocument, Array.prototype.map.call(keepableNode.getElementsByTagName("script"), function(node) { return node; }));

                                keepableNode.setAttribute("data-keep", "");  // flag as rendered
                            })
                        }
                    });
                }
            }
        };


        var sliceAtIndexOf = function(subject, separator)
        {
            var i = subject.indexOf(separator);
            if (i >= 0)
            {
                return [subject.slice(0, i), subject.slice(i)];
            }
            return [subject, ""];
        };


        var splitPath = function(pathHashAndQuery)
        {
            var url = { path: "", hash: "", query: ""};
            var parts = [pathHashAndQuery, ""];

            parts = sliceAtIndexOf(parts[0], '?');
            url.query = parts[1];

            parts = sliceAtIndexOf(parts[0], '#');
            url.hash = parts[1];
            url.path = parts[0];

            return url;
        };


        var splitRoute = function(canonicalPathWithArgs)
        {
            var routeDict = { routes: [], strippedPath: "" };

            var path = [];
            var args = [];
            var segments = canonicalPathWithArgs.split("/");
            for (var i = 0; i < segments.length; ++i)
            {
                var s = segments[i];
                if (s.startsWith(":"))
                {
                    args.push(s.slice(1));
                }
                else
                {
                    path.push(s);
                    routeDict.routes.push({ "path": path.join("/"), "args": args});
                    args = [];
                }
            }

            routeDict.strippedPath = path.join("/");

            return routeDict;
        };


        var navigateOnPopState = function(e)
        {
            // navigate to the "current" page
            var loc = window.location;
            app.navigate(loc.pathname + loc.hash + loc.search);
        };


        var navigate = function(relativePath)
        {
            var parts = splitPath(relativePath);
            var canonicalPathWithArgs = app.canonicalizePath(parts.path);
            var routeDict = splitRoute(canonicalPathWithArgs);

            // publish argument updates for all parent routes and this route
            for (var i = 0; i < routeDict.routes; ++i)
            {
                app.broker([ROUTE, routeDict.routes[i].path], function() { return Rx.Observable.of(routeDict.routes[i].args); }).pull();
            }

            var canonicalPath = routeDict.strippedPath;

            // notify subscribers about changed route
            app.broker(ROUTE, function() { return Rx.Observable.of(canonicalPath); }).pull();

            // copy reference to avoid some firefox bug
            var currentDocument = document;

            getDocument(canonicalPath).subscribe(function(newDocument)
            {
                try
                {
                    app.resetReadyState();

                    // place persistent elements with "keep=PARENT_ID" attribute
                    placeKeepAnnotated(currentDocument, newDocument);

                    // keep script urls to allow for de-duplication to avoid reloading scripts already present
                    var livePreviousScripts = currentDocument.getElementsByTagName("SCRIPT");
                    for (var i = 0; i < livePreviousScripts.length; ++i)
                    {
                        encounteredScriptUrls[livePreviousScripts[i].src] = null;
                    }

                    // perform document replace
                    currentDocument.replaceChild(newDocument.documentElement, currentDocument.documentElement);

                    // fix links in IE
                    if (currentDocument.createStyleSheet)
                    {
                        var links = currentDocument.getElementsByTagName("LINK");
                        for (var i = 0; i < links.length; ++i)
                        {
                            link.href = link.href;
                        }
                    }

                    // enforce script execution
                    forceScriptExecution(currentDocument, Array.prototype.filter.call(currentDocument.getElementsByTagName("script"), function(script)
                    {
                        if (script.parentNode.nodeName === "HEAD")
                        {
                            if (Object.prototype.hasOwnProperty.call(encounteredScriptUrls, script.src))
                            {
                                console.log("[ui] skip: ", script.src);
                                // skip this script
                                return false;
                            }
                        }
                        return true;
                    }));
                }
                catch (e)
                {
                    console.error("[ui] failed to construct target document: " + canonicalPath + ": ", e);
                /*
                    currentDocument = currentDocument.open("text/html");
                    currentDocument.write(newDocument.documentElement.outerHTML);
                    currentDocument.close();
                */
                }

                // change path to reflect actual location
                var title = undefined;
                var titleNode = currentDocument.getElementsByTagName("TITLE");
                if (titleNode.length > 0 && titleNode[0].childNodes.length > 0)
                {
                    title = titleNode[0].childNodes[0].nodeValue;
                };

                var targetPath = canonicalPathWithArgs + parts.hash + parts.query;
                window.history.pushState(undefined, title, targetPath);

                window.addEventListener("popstate", navigateOnPopState);

                app.requireAllLoaded(function() { window.dispatchEvent(new Event("load")); });

                console.log("[ui] navigated to: " + targetPath + ", title: " + title);
            },
            function(e)
            {
                console.error("[ui] failed to navigate to: " + canonicalPath + ": ", e);
            });

            console.log("[ui] navigate scheduled");
        };

        // publish methods
        Object.defineProperty(app, "navigate", { value: navigate });

        Object.defineProperty(app.subject, ROUTE, { value: ROUTE });


    })(window.app, document, window.Rx, window.app.broker);

});