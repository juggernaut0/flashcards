import org.w3c.dom.HTMLStyleElement
import kotlinx.browser.document

private const val col2 = "${200.0/12}%"
private const val col3 = "${300.0/12}%"
private const val col4 = "${400.0/12}%"
private const val col6 = "${600.0/12}%"
private const val col8 = "${800.0/12}%"
private const val col10 = "${1000.0/12}%"

private const val borderGray = "#ccc"
private const val btnColor = "#2a4"
private const val btnHoverColor = "#183"
private const val btnSecondaryColor = "#999"
private const val btnSecondaryHoverColor = "#666"
private const val btnDangerColor = "#d33"
private const val btnDangerHoverColor = "#c22"
private const val btnEditColor = "#25c" // TODO get lighter version
private const val btnEditHoverColor = "#25c"
private const val btnLightColor = "#eee"
private const val btnLightHoverColor = "#ddd"
private const val linkColor = "#444"
private const val linkHoverColor = "#222"
private const val lessonColor = "red"
private const val reviewColor = "#17b"

private const val css = """
body {
    margin: 0;
    font-family: "Segoe UI",Roboto,sans-serif;
    color: #222;
}
    
.container {
    display: flex;
    flex-wrap: wrap;
    box-sizing: border-box;
    border: 1px solid $borderGray;
    background-color: #f7f7f7;
    overflow: clip;
    padding: 0.5rem;
    margin: 0.5rem;
}

.sets-list-item {
    flex-basis: 100%;
    padding: 0.5rem 1rem;
    line-height: 2;
}

.sets-list-item:not(:last-child) {
    border-bottom: 1px solid $borderGray;
}

.set-header-box {
    display: flex;
    align-items: center;
    padding: 0 1rem;
    border-bottom: 1px solid $borderGray;
    width: 100%;
    box-sizing: border-box;
}

.set-header {
    flex-grow: 1;
    text-overflow: ellipsis;
    white-space: nowrap;
    overflow: hidden;
}

.icon-btn {
    font-size: 2rem;
    cursor: default;
}

.set-items {
    padding: 1rem;
}

.add-item-btn {
    width: 50%;
}

.buttons {
    display: flex;
    flex-wrap: wrap;
}

.dash-tile {
    display: block;
    width: 10rem;
    background-color: white;
    border: 1px solid $borderGray;
    border-radius: 0.25rem;
    margin: 0.25rem;
    padding: 0;
    overflow: hidden;
    height: 5.5rem;
}

.dash-tile:hover {
    background-color: #eee;
}

.dash-tile-add {
    color: #999;
    text-align: center;
    font-size: 36pt;
}

.dash-tile-content {

}

.dash-tile-title {
    text-overflow: ellipsis;
    overflow: hidden;
    white-space: nowrap;
}

.dash-tile-deck-indicators {
    display:flex;
    margin-top: 0.25rem;
    justify-content: space-evenly;
}

.indicator {
    padding: 0.125rem 0.25rem;
    border-radius: 0.25rem;
}

.indicator-lessons {
    background-color: $lessonColor;
    color: white;
}

.indicator-reviews {
    background-color: $reviewColor;
    color: white;
}

.source-type {
    border: 1px solid $borderGray;
    background-color: $btnLightColor;
    line-height: 2rem;
    margin-right: -1px;
}

.source-type:hover {
    background-color: $btnLightHoverColor;
}

.source-type-active {
    background-color: $btnLightHoverColor;
}

.source-type:first-child {
    border-top-left-radius: 0.25rem;
    border-bottom-left-radius: 0.25rem;
}

.source-type:last-child {
    border-top-right-radius: 0.25rem;
    border-bottom-right-radius: 0.25rem;
}

.button-confirm {
    background-color: $btnColor;
    color: white;
    border: 0;
    border-radius: 0.25rem;
    line-height: 1.5;
    padding: 0.375rem 0.75rem;
}

.button-confirm:hover {
    background-color: $btnHoverColor;
}

.button-delete {
    background-color: $btnDangerColor;
    color: white;
    border: 0;
    border-radius: 0.25rem;
    line-height: 1.5;
    padding: 0.375rem 0.75rem;
}

.button-delete:hover {
    background-color: $btnDangerHoverColor;
}

.error-alert {
    color: $btnDangerColor;
}

.sticky-top {
    position: -webkit-sticky;
    position: sticky;
    top: 0;
    z-index: 1;
}

.solid-row {
    background-color: #f7f7f7;
    flex-basis: 100%;
}

.gapped-row {
    display: flex;
    gap: 0.25rem;
    align-items: center;
}

.card-group {
    border: 1px solid $borderGray;
    border-radius: 0.25rem;
    overflow: clip;
    margin-bottom: 0.5rem;
}

.card-group-card {
    display: flex;
    padding: 0.25rem;
    line-height: 1.5rem;
    border-bottom: 1px solid $borderGray;
}

.card-info {
    display: flex;
    flex-grow: 1;
}

.card-info span {
    flex-grow: 1;
    width: 0;
    text-overflow: ellipsis;
    overflow: hidden;
    white-space: nowrap;
}

.card-button {
    border: 0;
    background-color: transparent;
    color: $borderGray;
    font-weight: bold;
    font-size: 20;
}

.card-button-del:hover {
    color: $btnDangerHoverColor;
}

.card-button-edit:hover {
    color: $btnEditHoverColor;
}

.button-add-card {
    background-color: $btnLightColor;
    border: 0;
    line-height: 1.5rem;
    text-align: center;
    width: 100%;
    padding: 0.25rem;
}

.button-add-card:hover {
    background-color: $btnLightHoverColor;
}

.button-add-group {
    background-color: $btnLightColor;
    border: 1px solid $borderGray;
    line-height: 1.5rem;
    text-align: center;
    width: 100%;
    padding: 0.25rem;
    border-radius: 0.25rem;
}

.button-add-group:hover {
    background-color: $btnLightHoverColor;
}

.deck-add-source-select {
    background: white;
    border: 1px solid $borderGray;
    border-top-left-radius: 0.25rem;
    border-bottom-left-radius: 0.25rem;
    padding: 0.25rem;
    min-width: 10rem;
}

.deck-add-source-button {
    background-color: $btnLightColor;
    border: 1px solid $borderGray;
    border-top-right-radius: 0.25rem;
    border-bottom-right-radius: 0.25rem;
    text-align: center;
    padding: 0.25rem;
}

.deck-add-source-button:hover {
    background-color: $btnLightHoverColor;
}

div,button {
    box-sizing: border-box;
}

.form-input {
    width: 100%;
    border: 1px solid $borderGray;
    border-radius: 0.25rem;
    font-size: 1rem;
    padding: 0.25rem 0.5rem;
    margin-top: 0.4rem;
}

h2,h3 {
    display: block;
    width: 100%;
}

label {
    display: flex;
    align-items: center;
    width: 100%;
    margin-bottom: 0.5rem;
    flex-wrap: wrap;
}

.row {
    padding: 0.25rem 0;
    flex: 0 0 100%;
}

.blur {
    filter: blur(4px);
}

.link-button {
    background-color: transparent;
    border: 0;
    color: $linkColor;
    cursor: pointer;
}

.link-button:hover {
    color: $linkHoverColor;
}

.review-main {
    width: 100%;
    background-color: #666;
    color: white;
    text-align: center;
    font-size: 6em;
    line-height: 3em;
    text-shadow: 5px 5px #555;
}

.review-prompt {
    width: 100%;
    background: #ccc;
    text-align: center;
    font-size: 1.5em;
    line-height: 2em;
    cursor: default;
}

.review-input {
    text-align: center;
    font-size: 1.5em;
    line-height: 2em;
    border: 0;
    border-top: 1px solid $borderGray;
    border-bottom: 1px solid $borderGray;
    width: 100%;
}

.review-input:focus {
    outline: none;
    box-shadow: none;
}

.review-input-correct {
    background-color: #2c2;
    color: white;
}

.review-input-incorrect {
    background-color: #c22;
    color: white;
}

.review-button-container {
    border: 1px solid $borderGray;
    border-top: 0;
    margin: 0 auto;
    max-width: max-content;
}

.review-button {
    background-color: $btnLightColor;
    line-height: 1.5em;
    width: 6em;
    border: 0;
    border-left: 1px solid $borderGray;
}

.review-button:hover:enabled {
    background-color: $btnLightHoverColor;
}

.review-button:first-child {
    border-left: 0;
}

.review-input-container {
    justify-content: center;
    align-items: center;
    position: relative;
    display: flex;
}

.review-mistake-tooltip {
    position: absolute;
    background-color: #eee;
    border: 1px solid $borderGray;
    top: -24px;
    padding: 0.25rem;
    cursor: default;
}

.review-mistake-hidden {
    display: none;
}

.review-summary-incorrect {
    color: #c22;
}

.review-summary-incorrect:before {
    content: "× "
}

.start-lesson-button {
    line-height: 5rem;
    width: 12rem;
    color: $lessonColor;
    background-color: white;
    border: 1px solid $lessonColor;
}

.start-lesson-button:hover:enabled {
    color: white;
    background-color: $lessonColor;
}

.start-lesson-button:disabled {
    color: $borderGray;
    border-color: $borderGray;
}

.start-review-button {
    line-height: 5rem;
    width: 12rem;
    color: $reviewColor;
    background-color: white;
    border: 1px solid $reviewColor;
    margin-left: 0.5rem;
}

.start-review-button:hover:enabled {
    color: white;
    background-color: $reviewColor;
}

.start-review-button:disabled {
    color: $borderGray;
    border-color: $borderGray;
}

.review-notes-panel {
    margin: 0 0.5rem;
    margin-top: -1px;
    border: 1px solid $borderGray;
    padding: 0.5rem;
}

.button-next-lesson {
    flex-basis: 50%;
    margin: auto;
}

"""

private const val modalCss = """
.modal-background {
    background-color: rgba(51, 51, 51, 0.5);
    position: fixed;
    left: 0;
    top: 0;
    width: 100%;
    height: 100%;
    display: none;
    justify-content: center;
    align-items: center;
    z-index: 1000;
}
.modal-box {
    background-color: white;
    font-family: "Segoe UI",Roboto,sans-serif;
    color: #222;
    padding: 1rem;
    border: 1px solid $borderGray;
    border-radius: 0.25rem;
    width: 80%;
}
.modal-show {
    display: flex;
}
.modal-btns {
    display: flex;
    margin: 0 -0.25rem;
}
.modal-btn {
    flex-basis: 50%;
    flex-grow: 1;
    height: 3rem;
    color: #fff;
    border: 0;
    border-radius: 0.25rem;
    margin: 0.25rem;
    font-size: 1rem;
}
.modal-btn-ok {
    background-color: $btnColor;
}
.modal-btn-ok:hover {
    background-color: $btnHoverColor;
}
.modal-btn-danger {
    background-color: $btnDangerColor;
}
.modal-btn-danger:hover {
    background-color: $btnDangerHoverColor;
}
.modal-btn-cancel {
    background-color: $btnSecondaryColor;
}
.modal-btn-cancel:hover {
    background-color: $btnSecondaryHoverColor;
}
@media only screen and (min-width: 600px) {
    .modal-box {
        width: 500px;
    }
}
"""

private const val tagInputCss = """
.taginput {
    width: 100%;
    display: flex;
    flex-wrap: wrap;
    box-sizing: border-box;
    padding: 0.25rem 0.5rem;
    border: 1px solid $borderGray;
    border-radius: 0.25rem;
    margin-top: 0.4rem;
    background-color: #fff;
    cursor: text;
}

.taginput-tag {
    padding-left: 0.25rem;
    background-color: $btnColor;
    color: #fff;
    border-radius: 0.25rem;
    margin: 0.25rem 0;
    margin-right: 0.5rem;
    cursor: default;
    font-size: 15px;
}

.taginput-tag-close {
    display: inline;
    border: 0;
    background-color: inherit;
    color: inherit;
    font-size: inherit;
    padding: 0.25rem;
    border-radius: 0 0.25rem 0.25rem 0;
    margin-left: 0.25rem;
}

.taginput-tag-close:hover {
    background-color: $btnHoverColor;
}

input.taginput-input {
    border: 0;
    line-height: 2;
    font-size: 1rem;
    flex-grow: 1;
    width: unset;
    order: 1;
}

.taginput-input:focus {
    outline: none;
}
"""

fun applyStyles() {
    appendCss(css)
    appendCss(modalCss)
    appendCss(tagInputCss)
}

private fun appendCss(css: String) {
    val styleElem = document.createElement("style") as HTMLStyleElement
    styleElem.type = "text/css"
    styleElem.innerHTML = css
    document.head?.appendChild(styleElem)
}
