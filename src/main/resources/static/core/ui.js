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
    "core/http.js",
],
function()
{
    console.log("[ui] init");

    (function(app, document, Rx)
    {
        var html5doctype = document.implementation.createDocumentType( 'html', '', '');
        var keepStore = document.implementation.createDocument('', 'html', html5doctype);

        var cloneKeepAnnotated = function(sourceDocument, targetDocument)
        {
            var nodesToKeep = [];
            var nodesToKeepFromCurrent = sourceDocument.querySelectorAll("[keep]");
            for (var i = 0; i < nodesToKeepFromCurrent.length; ++i)
            {
                nodesToKeep.push(nodesToKeepFromCurrent[i]);
            }
            var nodesToKeepFromStore = keepStore.querySelectorAll("[keep]");
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
                var parentId = sourceNode.attributes.keep.value;
                if (parentId)
                {
                    var newParent = targetDocument.getElementById(parentId);
                    var importTarget = targetDocument;
                    if (!newParent)
                    {
                        if (sourceNode.ownerDocument === keepStore)
                        {
                            // nothing to do
                            continue;
                        }

                        // keep stashed away in store
                        newParent = keepStore.documentElement;
                        importTarget = keepStore;
                    }

                    var node = importTarget.importNode(sourceNode, true);
                    newParent.appendChild(node);
                    sourceNodeParent.removeChild(sourceNode);
                }
                else
                {
                    console.warn("[ui] attribute keep without ID on element: ", sourceNode.nodeName);
                }
            }
        };

        var switchDocument = function(relativePath)
        {
            // copied from
            var currentDocument = document;

            this.GET(relativePath, "document", {}).subscribe(function(newDocument)
            {
                console.log("1");
                cloneKeepAnnotated(currentDocument, newDocument);
                try
                {
                    // keep script urls to allow for de-duplication to avoid reloading scripts already present
                    var livePreviousScripts = newDocument.getElementsByTagName("SCRIPT");
                    var previousScriptSources = [];
                    for (var i = 0; i < livePreviousScripts.length; ++i)
                    {
                        previousScriptSources.push(livePreviousScripts[i].src);
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
                            if (previousScriptSources.indexOf(script.src) >= 0)
                            {
                                console.log("[ui] skip refreshing: ", script.src);
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
                var title = currentDocument.getElementsByTagName("TITLE");
                if (title.length > 0)
                {
                    title = title.value;
                }
                var canonicalPath = app.canonicalizePath(relativePath);
                window.history.pushState({"url": canonicalPath}, title, canonicalPath);
            },
            function(e)
            {
                console.log("document switch failed");
                console.error("failed to request: " + relativePath + ": " + e.toString());
            });

            console.log("[ui] switchDocument scheduled");
        };

        // publish methods
        Object.defineProperty(app, "switchDocument", { value: switchDocument });

    })(window.app, document, window.Rx);

});