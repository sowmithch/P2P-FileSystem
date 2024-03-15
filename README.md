################ Java 7 or above is required #################

To Run the Peer we should pass ID as the argument(ID's can start from 0)

Example - java Server 0

Each server will have dedicated ports and their data is in the file (./meta/ports.txt)

Inorder to run multiple peers concurrently we should replicate all the files to a seperate directory and should start Server from there.

Upon starting the server Login and Register options will be displayed

First time user - 
Click on Register 
Provide username and password

Returning user -
Select Login
Provide username and password

Upon user registration/Login - select the functionality to access the file system

While granting the permission for user we have (Owner-Read-Write)

0 - False
1 - True

For example if user wanted to grant Read and write permissions it will be -- (011). If user want to grant Owner, Read and write permissions it will be (111)

Permission for the user 

List All Files – This function will allow users to check all the files that particular user can Access
Create - This function helps the user to create a file by a new name.
Write - This functionality will help the user to write data into a new or an existing file in the file system.
Read - This functionality will allow users to only read the data from an existing file
Delete -Delete is the function used when the user wants to delete the file.
Grant - This is the functionality that can only be performed by the owners of that file.
Revoke - This is also a functionality which can be performed by owners of that file.
Create Directory - The users can create a directory where they want to store all those files.
Delete Directory - The Users can delete the directory.







