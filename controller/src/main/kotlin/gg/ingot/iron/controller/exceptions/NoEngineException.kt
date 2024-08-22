package gg.ingot.iron.controller.exceptions

import gg.ingot.iron.representation.DBMS

class NoEngineException(dbms: DBMS): Exception(
    "No engine was found for ${dbms.value}, it probably isn't supported, you can register your " +
    "own with DBMSEngine.register(DBMSEngine)"
)