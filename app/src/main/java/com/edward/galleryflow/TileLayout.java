package com.edward.galleryflow;

/** Pure geometry, shared by the renderer and the JVM regression checks. */
public final class TileLayout {
    public static final class Box {
        public final float x, y, w, h;
        Box(float x, float y, float w, float h) { this.x=x; this.y=y; this.w=w; this.h=h; }
    }
    public static Box[] create(float width, float gap, float density, float[] ratios, boolean square) {
        Box[] out = new Box[ratios.length];
        if (square) {
            int cols = Math.max(1, Math.round(density));
            float size = (width-gap*(cols-1))/cols;
            for(int i=0;i<out.length;i++) out[i]=new Box((i%cols)*(size+gap),(i/cols)*(size+gap),size,size);
            return out;
        }
        float target = width/density, y=0;
        int start=0;
        while(start<ratios.length) {
            int end=start; float sum=0;
            do { sum+=ratio(ratios[end++]); }
            while(end<ratios.length && sum*target+gap*(end-start-1)<width);
            float height=(width-gap*(end-start-1))/sum;
            if(end==ratios.length) height=Math.min(height,target);
            float x=0;
            for(int i=start;i<end;i++) {float w=ratio(ratios[i])*height;out[i]=new Box(x,y,w,height);x+=w+gap;}
            y+=height+gap;start=end;
        }
        return out;
    }
    private static float ratio(float r) { return Float.isFinite(r)?Math.max(.55f,Math.min(2f,r)):1f; }
    public static Box interpolate(Box a, Box b, float t) {
        if(t<=0)return a;if(t>=1)return b;
        return new Box(a.x+(b.x-a.x)*t,a.y+(b.y-a.y)*t,a.w+(b.w-a.w)*t,a.h+(b.h-a.h)*t);
    }
    public static Box[] masonry(float width,float gap,int columns,float[] ratios,boolean[] headings,float headerHeight){
        Box[] boxes=new Box[ratios.length];float[] bottoms=new float[columns];float cell=(width-gap*(columns-1))/columns;
        for(int i=0;i<ratios.length;i++){
            if(headings[i]){float bottom=0;for(float b:bottoms)bottom=Math.max(bottom,b);boxes[i]=new Box(0,bottom,width,headerHeight);java.util.Arrays.fill(bottoms,bottom+headerHeight);continue;}
            int col=0;for(int c=1;c<columns;c++)if(bottoms[c]<bottoms[col])col=c;
            float r=Float.isFinite(ratios[i])?Math.max(columns==1?.005f:.12f,Math.min(columns==1?200f:5f,ratios[i])):1f;float h=cell/r;
            boxes[i]=new Box(col*(cell+gap),bottoms[col],cell,h);bottoms[col]+=h+gap;
        }return boxes;
    }
}
