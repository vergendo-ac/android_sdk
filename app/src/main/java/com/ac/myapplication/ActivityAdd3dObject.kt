package com.ac.myapplication

import com.google.ar.core.Pose
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.TransformableNode

class ActivityAdd3dObject : ActivityAdd2dObject() {

    override fun addObject(obj: ArObject, syncPose: Pose) {
        ModelRenderable.builder()
            .setSource(this, R.raw.star)
            .build()
            .thenAccept {
                val node = TransformableNode(arFragment.transformationSystem)
                val anchorNode = createAnchorNode(obj, syncPose)
                node.apply {
                    localRotation =
                        com.google.ar.sceneform.math.Quaternion.axisAngle(Vector3(0f, 1f, 1f), 90f)
                    renderable = it
                    setParent(anchorNode)
                    select()
                }
                obj.node = node
            }

    }
}
