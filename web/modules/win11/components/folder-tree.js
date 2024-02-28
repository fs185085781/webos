/*文件管理器-左侧递归组件*/
export default {
    template: `
        <div class="folder-tree-component dropdownmenu" v-for="node in nodes">
            <div class="droptitle">
                <div class="uicon arrUi" @click="zkAction(node)">
                    <svg class="svg-inline--fa fa-chevron-right" style="height:10px;width:10px;">
                        <use :xlink:href="'#arrow-'+(node.starZk?'b':'r')"></use>
                    </svg>
                </div>
                <div class="navtitle flex" @click="$emit('path-click',node.path)"><div class="uicon mr-1 ">
                    <img width="16" data-click="false" data-flip="false" data-invert="false" data-rounded="false" :src="node.icon" alt="">
                </div>
                    <span>{{node.name}}</span>
                </div>
            </div>
            <div class="dropcontent" v-if="node.starZk && node.children && node.children.length>0">
                <folder-tree :nodes="node.children" @path-click="pathClick"></folder-tree>
            </div>
        </div>
    `,
    props: ['nodes'],
    methods:{
        zkAction:async function (node){
            var that = this;
            if(!node.starZk && !node.children){
                var data = await webos.fileSystem.getFileListByParentPath(node.path);
                var list = data.contentFiles.filter(function (item){
                    return item.type == "2";
                });
                list.forEach(function (item){
                    item.name = item.filterName;
                    if(!item.thumbnail){
                        item.thumbnail = "modules/win11/imgs/folder-sm.png";
                    };
                    item.icon = item.thumbnail;
                    item.starZk = false;
                });
                node.children = list;
            }
            node.starZk = !node.starZk;
        },
        pathClick:function (path){
            this.$emit('path-click',path)
        }
    },
    created: function () {
    }
}