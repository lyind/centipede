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
    "core/http.js"
],
function()
{
    console.log("[ui] init");

    (function(app, document, Rx, broker)
    {
        var html5doctype = document.implementation.createDocumentType( 'html', '', '');
        var keepStore = document.implementation.createDocument('', 'html', html5doctype);
        var encounteredScriptUrls = {};


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
                        app.require(src, function() { keepableNode.setAttribute("data-keep", ""); });
                    });
                }
            }
        };


        // lookup the template element with "id", check if it has not been rendered before
        // and call onRender passing the element to fill as first argument
        var renderKeepable = function(id, onRender)
        {
            app.schedule(function(app, root)
            {
                var keepableElement = document.getElementById(id)
                if (keepableElement)
                {
                    if (keepableElement.getAttribute("data-keep") != null)
                    {
                        onRender(keepableElement);
                        keepableElement.setAttribute("data-keep", "");  // avoid re-render
                        console.log("[ui] keepable rendered: " + id);
                    }
                }
            });
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
                app.broker(["route", routeDict.routes[i].path], function() { return Rx.Observable.of(routeDict.routes[i].args); }).pull();
            }

            var canonicalPath = routeDict.strippedPath;

            // notify subscribers about changed route
            app.broker("route", function() { return Rx.Observable.of(canonicalPath); }).pull();

            // copied from
            var currentDocument = document;

            this.GET(canonicalPath, "document", {}).subscribe(function(newDocument)
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
                    var liveScripts = currentDocument.getElementsByTagName("SCRIPT");
                    var scripts = [];
                    var basePath = window.location;
                    for (var i = 0; i < liveScripts.length; ++i)
                    {
                        var script = liveScripts[i];
                        if (script.parentNode.nodeName === "HEAD")
                        {
                            if (Object.prototype.hasOwnProperty.call(encounteredScriptUrls, script.src))
                            {
                                console.log("[ui] skip: ", script.src);
                                continue;  // skip this script
                            }
                        }
                        scripts.push(script);
                    }

                    for (var i = 0; i < scripts.length; ++i)
                    {
                        var script = scripts[i];
                        var parent = script.parentNode;

                        var newScript = currentDocument.createElement("script");

                        // copy attributes
                        for (var j = 0; j < script.attributes.length; ++j)
                        {
                            var attribute = script.attributes[j];
                            newScript.setAttribute(attribute.name, attribute.value);
                        }

                        newScript.appendChild(currentDocument.createTextNode(script.innerHTML));

                        parent.replaceChild(newScript, script);
                    }
                }
                catch (e)
                {
                    currentDocument = currentDocument.open("text/html");
                    currentDocument.write(newDocument.documentElement.outerHTML);
                    currentDocument.close();
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
        Object.defineProperty(app, "renderKeepable", { value: renderKeepable });


    })(window.app, document, window.Rx, window.app.broker);

});