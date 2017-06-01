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
        var broker = function(subject, supplier)
        {
            subject = normalizeSubject(subject);

            var channel = store[subject];
            if (!channel)
            {
                channel = createChannel(function()
                {
                    // un-register from store after last subscriber disconnected
                    delete store[subject];
                });

                store[subject] = channel;
            }

            if (supplier)
            {
                channel.supplier = supplier;
            }

            return channel;
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
        var createChannel = function(onDereference)
        {
            return function(onDereference)
            {
                var onDereference = onDereference;
                var observer = this;
                var supplier = undefined;
                var subscription = undefined;
                var cache = new Rx.ReplaySubject(1);

                // output observable with added method request()
                var observable = cache.asObservable().finally(destructor).publishReplay(1).refCount();
                observable.pull = function()
                {
                    // un-subscribe from previous source
                    if (subscription)
                    {
                        subscription.unsubscribe();
                    }

                    var newValue = supplier();
                    subscription = newValue.subscribe(observer);
                    if (newValue.pull)
                    {
                        // allow broker subjects to connect to broker subjects
                        newValue.pull();
                    }
                };

                // implement Observer methods: closed(), next(), error() and complete()
                closed = function() { return false; };

                next = function(value) { cache.next(value); };

                error = function(e)
                {
                    console.log("channel encountered error");
                    cache.error(e);
                };

                complete = function()
                {
                    // ignore, we never complete
                };

                // destructor (called after the last subscriber disconnected)
                destructor = function()
                {
                    console.log("destroying value");

                    if (subscription)
                    {
                        subscription.unsubscribe();
                    }

                    if (onDereference)
                    {
                        onDereference();
                    }
                };
            }(onDereference);
        };

        // publish
        Object.defineProperty(app, "broker", { value: broker });

    })(window.app, window.Rx);
});