package com.edward.galleryflow;
final class FolderGridLayout {
 static TileLayout.Box[] create(float width,float gap,int columns,int count,float labelHeight){int cols=Math.max(1,columns);float side=Math.max(1,(width-gap*(cols+1))/cols),height=side+labelHeight;TileLayout.Box[] boxes=new TileLayout.Box[count];for(int n=0;n<count;n++)boxes[n]=new TileLayout.Box(gap+(n%cols)*(side+gap),gap+(n/cols)*(height+gap),side,height);return boxes;}
}
