var msg_id = argument0;
var buffer = argument1;

with(obj_client) {
	switch(msg_id) {
	
		case MSG_USER_ID:
			var uuid = buffer_read(buffer, buffer_string);
			obj_game.uuid = uuid;
			show_debug_message("Got UUID: " + string(obj_game.uuid));
			return true;
		
		default:
	        show_debug_message("Received Unknown Request ID");
	        return false;
	}
}