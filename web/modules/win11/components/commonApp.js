/**/
export default {
    template: `
        <div class="common-app-component">
        <iframe class="common-iframe" :src="win.data.url" :data-id="win.id" style="border:0px;width:100%;" :style="{'height':(win.height-40+(win.isSimple?41:0))+'px'}"></iframe>
        </div>
    `,
    props: ['win'],
    methods: {
    },
    created: function () {
    }
}