package com.example.gamemove

import com.example.dolphin.bussiness.parts.Border

class Move {
    // Default speed
    //cooX, cooY是移动目标的坐标，speed是速度，border是移动的边界
    var cooX: Double = 0.0
    var cooY: Double = 0.0
    var speed: Double = 1.0
    var border: Border = Border()
    fun Move(cooX: Double, cooY: Double,speed: Double, border: Border){
        this.cooX = cooX
        this.cooY = cooY
        this.speed = speed
        this.border = border
    }
    fun moveLeft(){
        if (cooX - speed >= border.leftUpX ) {
            cooX -= speed
        }
    }
    fun moveRight(){
        if (cooX + speed <= border.rightDownX){
            cooX += speed
        }
    }
    fun moveUp(){
        if (cooY - speed >= border.leftUpY){
            cooY -= speed
        }
    }
    fun moveDown(){
        if (cooY + speed <= border.rightDownY ){
            cooY += speed
        }
    }
}