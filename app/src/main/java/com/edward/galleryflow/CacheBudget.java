package com.edward.galleryflow;
final class CacheBudget {
 static int images(long heap){return (int)Math.max(12L<<20,Math.min(64L<<20,heap/6));}
 static int thumbnails(long heap){return (int)Math.max(4L<<20,Math.min(24L<<20,heap/12));}
}
