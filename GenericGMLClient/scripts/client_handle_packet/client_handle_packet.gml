var in_id = async_load[? "id"];

with(obj_client) {
	if(in_id == server) {
		var buffer = async_load[? "buffer"];
		var size = buffer_get_size(buffer);
	
		buffer_copy(buffer, 0, size, input_buffer, input_end_pointer);
		input_end_pointer += size;
	
		var bytes_buffered = (input_end_pointer - input_start_pointer);
		while(bytes_buffered >= 5) {
			var handshake = buffer_peek(input_buffer, input_start_pointer, buffer_u16);
			if(handshake == MSG_HANDSHAKE) {
				var msg_id = buffer_peek(input_buffer, input_start_pointer + 2, buffer_u8);
				var msg_data_size = buffer_peek(input_buffer, input_start_pointer + 3, buffer_u16);
				if(msg_data_size <= (bytes_buffered - 5)) {
					var handle_buffer = buffer_create(msg_data_size, buffer_fixed, 1);
					buffer_copy(input_buffer, input_start_pointer + 5, msg_data_size, handle_buffer, 0);
					client_handle_response(msg_id, handle_buffer);
					input_start_pointer += msg_data_size + 5;
					buffer_delete(handle_buffer);
				} else
					break;
			} else {
				// We've lost data somehow! This code should never run, and I've never seen it run.
				// But if it does run, it is a problem with GameMaker handling packets.
				//
				// The best we can do from this point is find the next header using the handshake.
				//
				// Alternatively, if your computer is *super slow* but your connection is *super fast* 
				// you could also be reaching here if the input_buffer completely fills up. 
				// Try increasing the size by factors of 2 until you reach something managable.
				// SEE client_create_connection
				input_start_pointer += 1;
			}
		
			bytes_buffered = (input_end_pointer - input_start_pointer);
		}
	
		if(input_start_pointer != 0) {
			var new_input_buffer = buffer_create(INPUT_BUFFER_MAX_SIZE, buffer_fixed, 1);
			buffer_copy(input_buffer, input_start_pointer, bytes_buffered, new_input_buffer, 0);
			buffer_delete(input_buffer);
			input_buffer = new_input_buffer;
			input_start_pointer = 0;
			input_end_pointer = bytes_buffered;
		}
	}
}