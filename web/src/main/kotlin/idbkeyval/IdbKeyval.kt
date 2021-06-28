@file:JsModule("idb-keyval")
@file:JsNonModule
package idbkeyval

import kotlin.js.Promise

external fun set(key: String, value: String): Promise<Unit>
external fun get(key: String): Promise<String?>
