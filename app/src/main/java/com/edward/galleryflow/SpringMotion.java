package com.edward.galleryflow;
/** Analytic damped spring: stable at arbitrary display refresh rates; units are pixels and seconds. */
final class SpringMotion {
 double value,velocity,target;final double frequency,damping;
 SpringMotion(double stiffness,double dampingRatio){frequency=Math.sqrt(stiffness);damping=dampingRatio;}
 void start(double position,double speed,double destination){value=position;velocity=speed;target=destination;}
 boolean step(double dt){dt=Math.max(0,Math.min(.064,dt));double y=value-target,w=frequency,z=damping,decay=Math.exp(-z*w*dt);
  if(z<1){double wd=w*Math.sqrt(1-z*z),a=y,b=(velocity+z*w*y)/wd,c=Math.cos(wd*dt),s=Math.sin(wd*dt);value=target+decay*(a*c+b*s);velocity=decay*((b*wd-z*w*a)*c+(-a*wd-z*w*b)*s);}
  else{double b=velocity+w*y;value=target+(y+b*dt)*decay;velocity=(velocity-w*b*dt)*decay;}
  if(Math.abs(value-target)<.35&&Math.abs(velocity)<4){value=target;velocity=0;return false;}return true;
 }
 static float rubber(float distance,float width){float d=Math.abs(distance),limit=Math.max(1,width);return Math.copySign((1-1/(d*.55f/limit+1))*limit,distance);}
 static int destination(float offset,float velocity,float width,boolean previous,boolean next){float projected=offset+velocity*.16f;int step=0;if(Math.abs(velocity)>550&&Math.abs(offset)>6)step=velocity<0?1:-1;else if(Math.abs(projected)>width*.23f)step=projected<0?1:-1;return step<0&&!previous||step>0&&!next?0:step;}
}
