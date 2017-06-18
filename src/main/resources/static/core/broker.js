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

// subject based observable broker
app.require([
    "lib/Rx.js"
],
function()
{
    console.log("[broker] init");

    (function(app, Rx)
    {
        var store = {};

        // get an observable for a specific subject
        // optionally set a new supplier function (must return an observable)
        var broker = function(subject, supplier, onlyIfEmpty)
        {
            subject = normalizeSubject(subject);

            var channel = store[subject];
            if (!channel)
            {
                channel = createChannel(subject, function()
                {
                    // un-register from store after last subscriber disconnected
                    delete store[subject];
                });

                store[subject] = channel;
            }

            if (supplier && (!onlyIfEmpty || channel.supplier === undefined))
            {
                channel.supplier = supplier;
            }

            return channel.observable;
        };


        // normalizes subject (array to string)
        var normalizeSubject = function(subject)
        {
            if (Array.isArray(subject))
            {
                return subject.join("/");
            }

            return subject;
        };


        // create a channel (an observer that never completes and has a ReplaySubject attached to it)
        var createChannel = function(subject, onDereference)
        {
            return function(onDereference)
            {
                var onDereference = onDereference;
                var observer = this;
                var _supplier = undefined;
                var subscription = undefined;
                var cache = new Rx.ReplaySubject(1);

                // destructor (called after the last subscriber disconnected)
                var destructor = function()
                {
                    console.log("[broker] destroying: " + subject);

                    if (subscription)
                    {
                        subscription.unsubscribe();
                    }

                    if (onDereference)
                    {
                        onDereference();
                    }
                };

                var channel = {};  // the observer

                // output observable with added method pull()
                var observable = cache.asObservable().finally(destructor).publishReplay(1).refCount();
                Object.defineProperty(observable, "pull", { value: function()
                {
                    // un-subscribe from previous source
                    if (subscription)
                    {
                        subscription.unsubscribe();
                    }

                    var newValue = (_supplier) ? _supplier() : Rx.Observable.empty();
                    subscription = newValue.subscribe(channel);
                    if (newValue.pull)
                    {
                        // allow broker subject to connect to broker subject
                        newValue.pull();
                    }

                    return observable;
                }});

                Object.defineProperty(channel, "observable", { get: function() { return observable; } });

                Object.defineProperty(channel, "supplier", {
                    set: function(newSupplier)
                    {
                        if ((typeof newSupplier) === 'function')
                        {
                            _supplier = newSupplier;
                        }
                        else
                        {
                            console.warn("[broker] ignored supplier for " + subject + " that is not a function: ", newSupplier);
                            console.trace();
                        }
                    },
                    get: function()
                    {
                        return _supplier;
                    }
                });

                Object.defineProperty(channel, "next", { value: function(value) { cache.next(value); }});

                Object.defineProperty(channel, "error", { value: function(e)
                {
                    console.log("[broker] encountered error for: " + subject);
                    cache.error(e);
                }});

                Object.defineProperty(channel, "complete", { value: function()
                {
                    // ignore, we never complete
                }});

                return channel;

            }(onDereference);
        };

        // publish
        Object.defineProperty(app, "broker", { value: broker });

        // broker subject id constants
        Object.defineProperty(app, "subject", { value: {} });

        // sub-protocol splitter registry
        Object.defineProperty(app, "splitters", { value: [] });

    })(window.app, window.Rx);
});