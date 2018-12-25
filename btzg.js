/*手机号js校验*/
function checkPhone() {
	var phone = $('#phone').val();
	phone = phone.replace(/\s/g, '');
	alert(phone);
	var regex = /^1[0-9]{10}$/;
	if (phone.match(regex)) {
		return true;
	} else {
		return false;
	}
}


/*图片上传回显*/
$('#img-upload').on('change', function() {
	var filePath = $(this).val(), //获取到input的value，里面是文件的路径
		fileFormat = filePath.substring(filePath.lastIndexOf(".")).toLowerCase();
	src = window.URL.createObjectURL(this.files[0]); //转成可以在本地预览的格式

	// 检查是否是图片
	if (!fileFormat.match(/.png|.jpg|.jpeg/)) {
		alert('上传错误,文件格式必须为：png/jpg/jpeg');
		return;
	}
	$('#img-echo').attr('src', src);
});