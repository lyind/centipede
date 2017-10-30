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

// reactive virtual scroll array adapter
app.require([
        "lib/Rx.js"
    ],
    function ()
    {
        console.log("[array-adapter] init");

        (function (app)
        {
            // Helper class for virtual scroll DOM list
            // Binds values of arrays emitted by the Observable source to copies of templateNode.
            // Use ArrayAdapter.added().subscribe() to render/recycle your items.
            function ArrayAdapter(document, templateNode, source, id)
            {
                // we inherit from Subscriber
                Rx.Subscriber.call(this, this);

                this.document = document;
                this.documentFragment = document.createDocumentFragment();
                this.managedNodes = [];
                this.container = templateNode.parentNode;
                this.previousValue = [];
                this.subscription = undefined;
                this.id = id;

                this.itemAddSubject = new Rx.Subject();
                this.addedObservable = this.itemAddSubject.asObservable().share();

                this.itemRemoveSubject = new Rx.Subject();
                this.removedObservable = this.itemRemoveSubject.asObservable().share();

                this.isNoScriptElement = function (node)
                {
                    return node.tagName && node.tagName.toUpperCase() !== "SCRIPT";
                };

                this.createChangeEvent = function (value, node, isAdd)
                {
                    var changeEvent = {};
                    Object.defineProperty(changeEvent, "value", {value: value});
                    Object.defineProperty(changeEvent, "template", {value: node});

                    if (isAdd)
                    {
                        // subscribers of ArrayAdapter.added() can use this to tear-down the item
                        Object.defineProperty(changeEvent, "removed", {
                            value: this.removedObservable
                                .filter(function (removed)
                                {
                                    return id(removed.value) === id(value);
                                })
                        });
                    }

                    return app.bindById(changeEvent, node);
                };

                // store template node in our fragment and remove from document
                if (templateNode.ownerDocument !== document)
                {
                    this.template = this.document.importNode(templateNode, true);
                }
                else
                {
                    this.template = this.container.removeChild(templateNode);
                }
                this.template.removeAttribute("hidden");
                this.template.removeAttribute("id");
                app.show(this.template);
                this.documentFragment.appendChild(this.template);

                this.subscription = source.subscribeOn(Rx.Scheduler.asap).subscribe(this);
            }

            ArrayAdapter.prototype = Object.create(Rx.Subscriber.prototype);

            ArrayAdapter.prototype.constructor = ArrayAdapter;

            ArrayAdapter.prototype.added = function ()
            {
                return this.addedObservable;
            };

            ArrayAdapter.prototype.removed = function ()
            {
                return this.removedObservable;
            };

            ArrayAdapter.prototype.next = function (value)
            {
                if (!value)
                    value = [];

                var i = 0;

                // first skip all items that we rendered before and that look the same
                for (; i < Math.min(value.length, this.previousValue.length) && this.id(this.previousValue[i]) === this.id(value[i]); ++i)
                {
                    // just don't touch these
                }

                var previousManagedNodesLength = this.managedNodes.length;

                // add templates if we don't have enough
                for (var j = previousManagedNodesLength; j < value.length; ++j)
                {
                    this.managedNodes.push(this.container.appendChild(this.document.importNode(this.template, true)));
                }

                // remove templates if we got too many
                for (var j = previousManagedNodesLength - 1; j >= value.length; --j)
                {
                    var node = this.container.removeChild(this.managedNodes.pop());

                    this.itemRemoveSubject.next(this.createChangeEvent(this.previousValue[j], node));
                }

                // render changed templates
                for (; i < value.length; ++i)
                {
                    // template contained something else already?
                    if (i < previousManagedNodesLength)
                    {
                        this.itemRemoveSubject.next(this.createChangeEvent(this.previousValue[i], this.managedNodes[i]));
                    }

                    this.itemAddSubject.next(this.createChangeEvent(value[i], this.managedNodes[i], true));
                }

                this.previousValue = value;
            };

            ArrayAdapter.prototype.error = function (e)
            {
                console.error("[array-adapter] error: ", e);
                this.itemAddSubject.error(e);
            };

            ArrayAdapter.prototype.complete = function ()
            {
                console.log("[array-adapter] complete");

                if (this.subscription)
                {
                    this.subscription.unsubscribe();
                    this.subscription = undefined;
                }
            };

            // Create a new ArrayAdapter using the specified arguments
            // source is the source observable which must emit arrays
            // id is the function used to retrieve a unique id per element
            Object.defineProperty(app, "arrayAdapter", {
                value: function (templateNode, source, id)
                {
                    // use a default id function in case none was provided
                    var idFunction = id || function(item) { return item; };

                    return new ArrayAdapter(window.document, templateNode, source, idFunction);
                }
            });

        })(window.app, window.document);
    });