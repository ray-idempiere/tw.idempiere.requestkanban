/**
 *  @author Ray Lee
 * 
 */

var isload = false;
var items;
var groups;
var options;
var timeline;
var container;
var cStart;
var ipqcDataJson;
options = {
	orientation: 'both',
	locale: 'en',
	zoomMin: 604800000,
	zoomMax: 3628800000,
	xss: { disabled: true },
	tooltip: {
		followMouse: false,
		overflowMethod: 'cap'
	},
	  editable: {
      add: true,
      remove: true,
      updateGroup: true,
      updateTime: true,
      overrideItems: false
    },
	onAdd: function(item, callback) {

		var item = item;
		var callback = callback;
		item.M_Production_ID = "0";
		let date = new Date(item.start);
    	date.setHours(date.getHours() + 24);
    	item.end = date.getTime();
		item.startTimestamp  = item.start.getTime().toString();
		item.endTimestamp  = date.getTime().toString();
		zk.$("$itemData").setValue(JSON.stringify(item));
		zk.$("$itemData").fireOnChange();
		zk.$("$dateLast").setValue(Date.now().toString());
        zk.$("$dateLast").fireOnChange();
		
		$("#update-production-form").dialog({
			title: "新增製令單",
			modal: true,
			width: "500px",
			buttons: {
				Ok: function() {
					$(this).dialog("close");

					zk.$("$itemData").setValue(JSON.stringify(item));
					zk.$("$itemData").fireOnChange();
					zk.$("$productionAdd").setValue(Date.now().toString());
					zk.$("$productionAdd").fireOnChange();
					item.content = "<img width='30' height='30' src='/webui/theme/zen/images/loading.gif'> ";
					callback(item);

				}, Cancel: function() {



					$(this).dialog("close");
					callback(null);

				}
			}
		});
	},

	onMove: function(item, callback) {
	        item.startTimestamp  = item.start.getTime().toString();
			item.endTimestamp  = item.end.getTime().toString();
            zk.$("$itemData").setValue(JSON.stringify(item));
		    zk.$("$itemData").fireOnChange();
			zk.$("$dateLast").setValue(Date.now().toString());
			zk.$("$dateLast").fireOnChange();

		callback(item);
	},

	onMoving: function(item, callback) {
		callback(item); // send back the (possibly) changed item

	},

	onUpdate: function(item, callback) {
		console.log("onUpdate");
		console.log(item);

		zk.$("$itemData").setValue(JSON.stringify(item));
		zk.$("$itemData").fireOnChange();
		zk.$("$dateLast").setValue(Date.now().toString());
        zk.$("$dateLast").fireOnChange();
		
		$("#documentNo").val(item.documentNo);
		$("#product").val(item.product);
		$("#productionQty").val(item.productionQty);
		$("#description").val(item.description);
		$("#group").val(item.group);
		$("#M_Production_ID").val(item.M_Production_ID);
		$("#update-production-form").dialog({
			title: "修改製令單",
			modal: true,
			width: "400px",
			buttons: {
				Ok: function() {
					$(this).dialog("close");

					zk.$("$itemData").setValue(JSON.stringify(item));
					zk.$("$itemData").fireOnChange();
					zk.$("$productionUpdated").setValue(Date.now().toString());
					zk.$("$productionUpdated").fireOnChange();

					callback(item);

				}, Cancel: function() {



					$(this).dialog("close");
					callback(null);

				}
			}
		});
	},
    onRemove: function (item, callback) {
    						 zk.$("$itemData").setValue(JSON.stringify(item));
    					     zk.$("$itemData").fireOnChange();
    						 
              				 zk.$("$productionDelete").setValue(Date.now().toString());
         					 zk.$("$productionDelete").fireOnChange();
         					 callback(item);
    }
};

function initChart() {
	//zk.$("$timeline-chart").uuid
	//container = document.getElementById("timeline-chart");
	container = document.getElementById(zk.$("$timeline-chart").uuid);
	timeline = new vis.Timeline(container, items, groups, options);
	initIPQC();
	zk.$("$loading").hide();
	//$("$loading").hide();
}
function drawChart() {

	timeline.setData({ groups: groups, items: items });
	timeline.redraw();
	initIPQC();
}

function initIPQC() {

	$(".vis-item-content .btn-ipqc").button();
	$(".vis-item-content .btn-ipqc").on("click", function(e) {
		var ipqcType = $(this).html();
		$("#ipqc-approve option[value!='']").each(function() {
			$(this).removeAttr('disabled');
			$(this).removeAttr('selected');
		});
		if (ipqcType == 'IPQC') {

			$("#ipqc-approve option[value='NGBB']").each(function() {
				$(this).attr('disabled', 'disabled');
			});
		} else {

			$("#ipqc-approve option[value='NGRW']").each(function() {
				$(this).attr('disabled', 'disabled');
			});

			$("#ipqc-approve option[value='NGST']").each(function() {
				$(this).attr('disabled', 'disabled');
			});

		}

		$("#ipqc-action").val("reload");
		$("#ipqc-production-id").val($(this).attr("M_Production_ID"));
		$("#ipqc-node").val($(this).html());
		$("#ipqc-memo").val("");
		$("#ipqc-product").val("");
		$("#ipqc-lot").val("");
		$("#ipqc-qty").val("");
		$("#ipqc-documentno").val("");
		const json = convertFormToJSON($("#ipqc-form"));
		zk.$("$ipqcData").setValue(JSON.stringify(json));
		zk.$("$ipqcData").fireOnChange();

		$("#ipqc-container").dialog({
			title: "IPQC",
			modal: true,
			buttons: {
				Ok: function() {

					if ($("#ipqc-approve").val() == '') {
						$.toast({
							heading: '操作異常',
							text: '未選擇 IPQC 審核項目',
							position: 'mid-center',
							stack: true
						});
						return;
					}

					$("#ipqc-action").val("save");

					$(this).dialog("close");

					const json = convertFormToJSON($("#ipqc-form"));
					/**
					將Form 資料轉換成 json 讓後端處理
					 */

					zk.$("$ipqcData").setValue(JSON.stringify(json));
					zk.$("$ipqcData").fireOnChange();

				}, Cancel: function() {
					$(this).dialog("close");

				}
			}, hide: { effect: "explode", duration: 1000 }
			, width: 500
		});

	}
	);
}
function convertFormToJSON(form) {
	const array = $(form).serializeArray(); // Encodes the set of form elements as an array of names and values.
	const json = {};
	$.each(array, function() {
		json[this.name] = this.value || "";
	});
	return json;
}

function reload() {
	ipqcDataJson = JSON.parse(zk.$("$ipqcData").getValue());
	$("#ipqc-product").val(ipqcDataJson.product);
	$("#ipqc-documentno").val(ipqcDataJson.documentno);
	$("#ipqc-qty").val(ipqcDataJson.qty);
	$("#ipqc-lot").val(ipqcDataJson.lot);
	$("#ipqc-memo").val(ipqcDataJson.memo);
}
function toTimestamp(value){
	const date = new Date(value);
	return  date.getTime();
}
