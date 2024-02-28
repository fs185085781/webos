(function(){
    window.player = {
        hasInList(list, data) {
            for (let i = 0; i < list.length; i++) {
                if(list[i].path == data.path){
                    return i;
                }
            }
            return -1;
        }
    }
})()