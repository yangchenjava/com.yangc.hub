Ext.define("Client", {
    extend: "Ext.data.Model",
    fields: [
		{name: "username",   type: "string"},
		{name: "channelId",  type: "int"}
    ]
});

Ext.onReady(function(){
	/** ------------------------------------- store ------------------------------------- */
	var store_clientGrid = Ext.create("Ext.data.Store", {
		model: "Client",
		pageSize: 20,
		proxy: {
			type: "ajax",
			actionMethods: {
				create: "POST", read: "POST", update: "POST", destroy: "POST"
			},
			url: basePath + "resource/bridge/getClientStatusList_page",
			reader: {
            	root: "dataGrid",
                totalProperty: "totalCount"
            }
		},
		autoLoad: true
	});
	
	/** ------------------------------------- view ------------------------------------- */
	var grid_client = Ext.create("Ext.grid.Panel", {
        renderTo: "client",
		store: store_clientGrid,
		width: "100%",
		height: document.documentElement.clientHeight - 125,
		border: false,
        collapsible: false,
        multiSelect: false,
        scroll: false,
        viewConfig: {
            stripeRows: true,
            enableTextSelection: true
        },
        columns: [
            {text: "用户名", flex: 1, align: "center", dataIndex: "username"},
            {text: "channelId", flex: 1, align: "center", dataIndex: "channelId"}
        ],
        tbar: new Ext.Toolbar({
        	height: 30,
			items: [
		        {width: 5,  disabled: true},
		        {width: 440, xtype: "label", id: "serverStatus"},
			    {width: 16, height: 16, xtype: "image", id: "serverActive"}
		    ]
        }),
        bbar: Ext.create("Ext.PagingToolbar", {
        	store: store_clientGrid,
            displayInfo: true,
            displayMsg: "当前显示{0} - {1}条，共 {2} 条记录",
            emptyMsg: "当前没有任何记录"
        })
    });
	
    /** ------------------------------------- handler ------------------------------------- */
    function serverStatus(){
    	$.post(basePath + "resource/bridge/getServerStatus", function(data){
			if (data) {
				Ext.getCmp("serverStatus").setText("服务器状态 - IP地址：" + data.ipAddress + "，端口：" + data.port + "，超时时间：" + data.timeout + "秒，连接状态：");
				if (data.active) {
					Ext.getCmp("serverActive").setSrc(basePath + "js/lib/ext4.2/icons/accept.png");
				} else {
					Ext.getCmp("serverActive").setSrc(basePath + "js/lib/ext4.2/icons/exception.png");
				}
			}
		});
    }
    serverStatus();
});
