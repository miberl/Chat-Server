# Chat-Server

RFC 'CHAT UDP'

General format of a packet:

    --------------------------------
    |  OPCODE  |  LENGTH  |  DATA  |
    --------------------------------
     01 bytes   02 bytes   n bytes
  
  
0. General confirmation / Error messages (Server to Client )
  
  0.1 'OK' -> Procedure completed correctly

      -------------
      |  0  |  0  |
      -------------

  0.2 'Error' -> An error was encountered during the completion of procedure

      ------------------------------------------
      |  01  |  *  |  ERROR_DESCRIPTION_STRING  |
      ------------------------------------------


1. Initial Connection Protocol

  1.1 Client to server registration

      --------------------------------
      |  11  |  LENGTH  |  USERNAME  |
      --------------------------------

  1.2 Server to client response:

    'OK' -> Procedure completed correctly, username was added to user list.
            Packet format as described in '0.1'

    'Error' -> User hasn't been added because of error.
            Possible errors are: username is already present, username is invalid.
            Packet format as described in '0.2'
  
2. Logout 
  
  2.1 Client to server disconnection
      
    Client sends disconnection request to the server.
      --------------
      |  12 |  00  |
      --------------
      
  2.2 Disconnection Acknowledgment
      
      The server, after deleting the client's username from the user-list responds with 'OK'. Format as described in 0.1.
      
    
3. Information
  
  3.1 Client server information request, to test status.
  
      ---------------
      |  40  |  00  |
      ---------------
      
  3.2 Server information response: server date and version.
    
      -------------------------------------------
      |  41  |  LENGTH  |  SERVER_DATE_VERSION  |
      -------------------------------------------
   
  3.3 Client list request 
  
      ---------------
      |  42  |  00  |
      ---------------
 
  3.4 Server list 
      
      Server sends list of users, if prompted by client request and at regular intervals
      ----------------------------------------------------------------
      |  43  |  LENGTH  |  USER_1  |  0  |  USER_2  |  0  |  USER_N  |
      ----------------------------------------------------------------
       

4. Public message

  4.1 Client sends 'broadcast' message to all users.
  
      ------------------------------
      |  20  |  LENGTH  |  MESSAGE |
      ------------------------------
      
  4.2 Server forwards packet to all connected users (Including sender).
  
      -------------------------------------------
      |  21  |  *  |  SENDER  |  0  |  MESSAGE  |
      -------------------------------------------
 

5. Private message 
      
  5.1 Client sends message to server, addressed to specific user.

      --------------------------------------------
      |  22  |  *  |  RECEIVER  |  0  |  MESSAGE |
      --------------------------------------------
      
  5.2 Server forwards message to specified user.
      
      -------------------------------------------
      |  23  |  *  |  SENDER  |  0  |  MESSAGE  | 
      -------------------------------------------

  Server to sender response:

    'OK' -> Procedure completed correctly, destination user is present in list.
            Packet format as described in '0.1'

    'Error' -> Procedure not completed, destination user wasn't found in list.
            Possible errors are: username is already present, username is invalid.
            Packet format as described in '0.2'
