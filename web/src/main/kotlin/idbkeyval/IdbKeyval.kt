@file:JsModule("idb-keyval")
@file:JsNonModule
package idbkeyval

import kotlin.js.Promise

external fun set(key: String, value: Any?): Promise<Unit>
external fun get(key: String): Promise<Any?>
