/* 
 * Copyright 2014 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

$(document).ready(function(){
  $(document).delegate("table.tree-node", "mouseenter", function() {
    var table = $(this)
    if (table.attr('draggable')!=='true') {
      console.log('making draggable')
      table.attr("draggable", 'true')
      table.bind('dragstart', function(ev){
        console.log('drag started')
        var data = ev.originalEvent.dataTransfer
        data.setData('text/plain', table.find('a span').attr('title'))
//        data.effectAllowed = 'none'
//        data.dropEffect = 'copy'
        console.log(data)
//        return true
      })
      table.bind('dragend', function(){
        return false
      })
    }
  })
})