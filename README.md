# Franklin

.. is you personal scala librarian API. He stores and finds the stuff you need, either to/from MongoDB for production purposes, or in-memory for testing.

No, but seriously, Franklin is just a wrapper for a subset of ReactiveMongo with the option to run in-memory instead of against a mongodb database. Franklin gets a few new APIs every now and then. Franklin APIs should never really expose any mongodb details - Franklin's API is just asynchronous document/kv-storage.

Extended and tested as needed. Not really meant for anyone else to use, but go ahead (MIT licensed) if you want to.
Very limited API really only intended to support [valhalla-game](https://github.com/saiaku-gaming/valhalla-server)
