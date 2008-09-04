/*
 Copyright (c) 2000 - 2008, The JAP-Team
 All rights reserved.
 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

  - Redistributions of source code must retain the above copyright notice,
 this list of conditions and the following disclaimer.

  - Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation and/or
 other materials provided with the distribution.

  - Neither the name of the University of Technology Dresden, Germany nor the names of its contributors
 may be used to endorse or promote products derived from this software without specific
 prior written permission.


 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS
 OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS
 BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 */
#import "JAPMacOSXLib.h"

static JavaVM* jvm;
static jmethodID dockMenuCallback_mID;
static jclass jJAPClass;

@implementation ApplicationDelegate
- (NSMenu*) applicationDockMenu:(NSApplication*) sender
{
	NSMenu* menu;
	NSMenu* submenu;
	NSMenuItem* item;
	
	menu = [NSMenu new];
	
	[menu addItemWithTitle: @"Info"
		  action: @selector(dockMenuCallback:)
		  keyEquivalent: @"i"];
		  
	[menu addItemWithTitle: @"Hide"
		  action: @selector(hide:)
		  keyEquivalent: @"h"];
		 
	[menu addItemWithTitle: @"Quit"
		  action: @selector(terminate:)
		  keyEquivalent: @"q"];
	
	item = [menu addItemWithTitle:@"Kaskaden" action:@selector(dockMenuCallback:) keyEquivalent: @"k"];
	submenu = [NSMenu new];
	
	[submenu addItemWithTitle: @"Speedpartner"
			 action: @selector(dockMenuCallback:)
			 keyEquivalent: @"s"];
	
	[submenu addItemWithTitle: @"Dresden-Dredsen"
			 action: @selector(dockMenuCallback:)
			 keyEquivalent: @"d"];
	
	[menu setSubmenu:submenu forItem:item];
	
	return menu;
}

- (void) dockMenuCallback:(id) sender
{
	JNIEnv *env = NULL;
	jint result = JNI_OK;
	bool bNeedsDetach = false;
	
	if(jvm)
	{
		result = (*jvm)->GetEnv(jvm, (void**) env, JNI_VERSION_1_4);
		if(result == JNI_EDETACHED) 
		{
			result = (*jvm)->AttachCurrentThread(jvm, (void**) env, NULL);
			if(result == JNI_OK) 
			{
				bNeedsDetach = true;
			}
		}
	}
	
	if(result != JNI_OK)
	{
		NSLog(@"dockMenuCallback: could not attack to JVM");
		return;
	}
	
	(*env)->CallStaticVoidMethod(env, jJAPClass, dockMenuCallback_mID);
	
	if(bNeedsDetach)
	{
		(*jvm)->DetachCurrentThread(jvm);
	}
}
@end

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) 
{
	jvm = vm;
	
	return JNI_VERSION_1_4;
}

// % javap -classpath JavaHeaders.jar -s gui.JAPMacOSXLib
JNIEXPORT void JNICALL Java_gui_JAPMacOSXLib_nativeInit(JNIEnv* env, jclass class)
{
	jJAPClass = (*env)->NewGlobalRef(env, class);
	dockMenuCallback_mID = (*env)->GetStaticMethodID(env, class, "dockMenuCallback", "()V");
}

JNIEXPORT void JNICALL Java_gui_JAPMacOSXLib_nativeInitDockMenu(JNIEnv* env, jclass class)
{
	NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
	
  	[NSApplication sharedApplication];
	
	[NSApp setDelegate: [ApplicationDelegate new]];
	
	[pool release];
}