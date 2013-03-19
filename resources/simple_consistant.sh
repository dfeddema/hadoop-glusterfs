#!/bin/sh
#
# Arbitrary text is copied to every node in the gluster cluster's gluster mount point.  It then compares the copied files with the local copy to see if the system has achieved
# consistancy when the copy command returns
#
# NODE_LIST is a list of the gluster cluster nodes/hostnames.  Each should have a authorized_keys entry to allow ssh from this node.
# GLUSTER_MOUNT is the pre-mounted gluster volume on every node.
#

NODE_LIST=("gluster1" "gluster2")
GLUSTER_MOUNT="/mnt/gluster"
LOCATION=`pwd`
ME=`hostname`

# Get this scripts name stripping off the ./ part

SCRIPT=${0:2}


for HOST in "${NODE_LIST[@]}"
do
   # Loop through all the nodes, do stuff if it's not the local node.
   if [ "${HOST}" != "${ME}" ]; then
      # Copy this script to every other node, specifically to the gluster mount on that node.
      DESTINATION="${GLUSTER_MOUNT}/${SCRIPT}-${HOST}"
      echo "Copy to ${DESTINATION}"
      scp "${LOCATION}/${SCRIPT}" "${HOST}:${DESTINATION}"

      #check that the copied script shows up on this node's gluster mount point, then diff the original + copied.
      diff "${DESTINATION}" "${LOCATION}/${SCRIPT}"
   fi
done
