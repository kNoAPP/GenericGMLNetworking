with(obj_client) {
	// DEFINE MESSAGE IDs
	MSG_HANDSHAKE = 420;
	MSG_USER_ID = 1;
	
	// Increase or decrease depending on your expected packet rate and handling speed.
	// I've found that this buffer size can handle about 40,000 bytes of information per
	// second.
	INPUT_BUFFER_MAX_SIZE = 131072;

	input_buffer = buffer_create(INPUT_BUFFER_MAX_SIZE, buffer_fixed, 1);
	input_start_pointer = 0;
	input_end_pointer = 0;

	server = network_create_socket(network_socket_tcp);
	network_set_timeout(server, 5000, 5000);
	var result = network_connect_raw(server, "127.0.0.1", 25500);
	// var result = network_connect_raw(server, "149.56.131.81", 25500);
	if(result >= 0) {
	    buffer = buffer_create(256, buffer_grow, 1);
	    show_debug_message("Connected to server successfully.");
		client_send_request(server, buffer, MSG_USER_ID); // Ask for UUID
	} else {
	    if(show_question("Couldn't connect to server, retry?")) {
	        network_destroy(server);
	        game_restart();
	    } else {
	        network_destroy(server);
	        game_end();
	    }
	}
}