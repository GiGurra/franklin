# Franklin

.. is you personal scala librarian API. He stores and finds the stuff you need, either to/from MongoDB for production purposes, or in-memory for testing.

No, but seriously, Franklin is just a wrapper for ReactiveMongo with in-memory capabilities. Franklin gets a few new APIs every now and then. Franklin APIs should never really expose any mongodb details - it should just be an asynchronous API to some document/kv-storage.

Extended and tested as needed. Not really meant for anyone else to use, but go ahead (MIT licensed) if you want to.
