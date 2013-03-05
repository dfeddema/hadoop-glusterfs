#A Shell script for testing on VMs
echo "Edit the branch value in this script to test a different branch"
export BRANCH=BZXXXX #Replace XXXX with the branch you intend to deploy.
git pull
git checkout $BRANCH
mvn -Dmaven.test.skip=true package
mv target/glusterfs-0.20.2-0.1.jar glustertest$BRANCH.jar
echo "Now copying jar to VMs"
#Edit the below lines if necessary to work on BAGL (i.e. change the server names).
scp glustertest$BRANCH.jar server-1:/usr/lib/hadoop/lib/a0glusterfs.jar
scp glustertest$BRANCH.jar server-2:/usr/lib/hadoop/lib/a0glusterfs.jar
scp glustertest$BRANCH.jar server-3:/usr/lib/hadoop/lib/a0glusterfs.jar
#...
echo "Done copying priority named jar to servers"
