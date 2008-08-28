#import "JAPMacOSXLib.h"

static JavaVM *jvm;
static jmethodID dockMenuCallback_mID;
static jclass jDelegateClass;
static NSMenu* menu;

// Determines whether the current thread is already attached to the VM,
// and tells the caller if it needs to later DetachCurrentThread 
//
// CALL THIS ONCE WITHIN A FUNCTION SCOPE and use a local boolean
// for mustDetach; if you do not, the first call might attach, setting 
// mustDetach to true, but the second will misleadingly set mustDetach 
// to false, leaving a dangling JNIEnv
jint GetJNIEnv(JNIEnv **env, bool *mustDetach) 
{
	jint getEnvErr = JNI_OK;
	*mustDetach = false;
	if (jvm) {
		getEnvErr = (*jvm)->GetEnv(jvm, (void **)env, JNI_VERSION_1_4);
		if (getEnvErr == JNI_EDETACHED) 
		{
			getEnvErr = (*jvm)->AttachCurrentThread(jvm, (void **)env, NULL);
			if (getEnvErr == JNI_OK) 
			{
				*mustDetach = true;
			}
		}
	}
	return getEnvErr;
}

@implementation AppDelegate
- (NSMenu *) applicationDockMenu:(NSApplication *)sender
{
	/*NSMenu* menu;
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
	
	[menu setSubmenu:submenu forItem:item];*/
	
	return menu;
}

- (void) dockMenuCallback:(id)sender
{
	JNIEnv *env = NULL;
	bool detach = false;
	
	if(GetJNIEnv(&env, &detach) != JNI_OK)
	{
		NSLog(@"dockMenuCallback: could not attack to JVM");
		return;
	}
	
	(*env)->CallStaticVoidMethod(env, jDelegateClass, dockMenuCallback_mID);
	
	if(detach)
	{
		(*jvm)->DetachCurrentThread(jvm);
	}
}
@end

// Delegate selector for openPanel selection
// Calls back to Java with result status: either cancellation or the path for a file to open
/*- (void) openPanelDidEnd:(NSOpenPanel *)sheet returnCode:(int)returnCode contextInfo:(void *)contextInfo {
	JNIEnv *env = NULL;
	bool shouldDetach = false;

	// Find out if we actually need to attach the current thread to obtain a JNIEnv, 
	// or if one is already in place
	// This will determine whether DetachCurrentThread should be called later
	if (GetJNIEnv(&env, &shouldDetach) != JNI_OK) {
		NSLog(@"savePanelDidEnd: could not attach to JVM");
		return;
	}
	
	// If we have file results, translate them to Java strings and tell the Java JAPMacOSXLib 
	// class to notify our listener
	if (returnCode == NSOKButton) {				
		NSEnumerator *files = [[sheet filenames] objectEnumerator];
		
		// init a jobjectArray with [[sheet filenames] count]
		jclass stringClass = (*env)->FindClass(env, "java/lang/String");
		jobjectArray jpaths = (*env)->NewObjectArray(env, [[sheet filenames] count], stringClass, NULL);
		
		NSString *nextFile;
		int javaIndex = 0;
		while (nextFile = (NSString *)[files nextObject]) {
			jsize buflen = [nextFile length];
			jchar buffer[buflen];
			[nextFile getCharacters:(unichar *)buffer];
			// fill the jobjectArray
			jstring js = (*env)->NewString(env, buffer, buflen);
			if (js == NULL) {
				NSLog(@"openPanelDidEnd: could not create jstring");
				return;
			}
			(*env)->SetObjectArrayElement(env, jpaths, javaIndex++, js);
			(*env)->DeleteLocalRef(env, js);
		}
		
		// Tell Java to notify the SheetListener
		if (openFinish_mID != NULL) {
			(*env)->CallStaticVoidMethod(env, jDelegateClass, openFinish_mID, sheetListener, jpaths);
		} else NSLog(@"openPanelDidEnd: bad mID");
		
		(*env)->DeleteLocalRef(env, jpaths);
		(*env)->DeleteLocalRef(env, stringClass);
	} else if (cancel_mID != NULL) {
		// Cancellation callback; Java will invoke sheetCancelled on our listener
		(*env)->CallStaticVoidMethod(env, jDelegateClass, cancel_mID, sheetListener);
	}
	
	// We're done with the listener; release the global ref
	(*env)->DeleteGlobalRef(env, sheetListener);

	// IMPORTANT: if GetJNIEnv attached for us, we need to detach when done
	if (shouldDetach) {
		(*jvm)->DetachCurrentThread(jvm);
	}
	// This delegate was was retained in nativeShowSheet; since this callback occurs on the AppKit thread,
	// which always has a pool in place, it can be autoreleased rather than released
	[self autorelease];
}*/


JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) 
{
	jvm = vm;
	
	return JNI_VERSION_1_4;
}

// Cache all our callbacks at class-load time
// These may be invalidated if the Java class is unloaded, so we globalRef the class
// To get native signatures for Java methods:
//	% cd <build dir>
//	% javap -classpath JavaHeaders.jar -s gui.JAPMacOSXLib
JNIEXPORT void JNICALL Java_gui_JAPMacOSXLib_nativeInit
  (JNIEnv * env, jclass clazz) 
{
	jDelegateClass = (*env)->NewGlobalRef(env, clazz);
	dockMenuCallback_mID = (*env)->GetStaticMethodID(env, clazz, "dockMenuCallback", "()V");
}

JNIEXPORT void JNICALL Java_gui_JAPMacOSXLib_nativeInitDockMenu
  (JNIEnv * env, jclass class)
{
	NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
	
  	[NSApplication sharedApplication];
	
	[NSApp setDelegate: [AppDelegate new]];
	
	[pool release];
}

JNIEXPORT void JNICALL Java_gui_JAPMacOSXLib_setMenu(JNIEnv* env, jclass class, jobject obj)
{
	menu = [NSMenu new];
	[menu retain];
	
	jclass jMenuClass = (*env)->FindClass(env, "javax/swing/JMenu");
	jclass jMenuItemClass = (*env)->FindClass(env, "javax/swing/JMenuItem");
	
	if(!(*env)->IsInstanceOf(env, obj, jMenuClass))
	{
		NSLog(@"setMenu: object is not a JMenu");
		return;
	}
	
	jmethodID getMenuComponentCount_mID = (*env)->GetMethodID(env, jMenuClass, "getMenuComponentCount", "()I");
	jmethodID getMenuComponent_mID = (*env)->GetMethodID(env, jMenuClass, "getMenuComponent", "(I)Ljava/awt/Component;");
	jmethodID getText_mID = (*env)->GetMethodID(env, jMenuItemClass, "getText", "()Ljava/lang/String;");
	
	if(getMenuComponentCount_mID == 0)
	{
		NSLog(@"setMenu: could not find method JMenu_getMenuComponentCount");
		return;
	}
	
	if(getMenuComponent_mID == 0)
	{
		NSLog(@"setMenu: could not find method JMenu_getMenuComponent");
		return;
	}

	if(getText_mID == 0)
	{
		NSLog(@"setMenu: could not find method AbstractButton_getText");
		return;
	}

	jint count = (*env)->CallIntMethod(env, obj, getMenuComponentCount_mID);
	jint i;
	
	for(i = 0; i < count; i++)
	{
		jobject item = (*env)->CallObjectMethod(env, obj, getMenuComponent_mID, i);
		
		if(item == NULL)
		{
			NSLog(@"setMenu: unknown error. skipping menu entry %i", i);
			continue;
		}
		
		jstring text = (jstring) (*env)->CallObjectMethod(env, item, getText_mID);
		const char* buffer = (*env)->GetStringUTFChars(env, text, NULL);
		NSString* string = [NSString stringWithUTF8String:buffer];
		
		NSLog(@"setMenu: adding menu item '%s'", buffer);
		[menu addItemWithTitle: string action: @selector(dockMenuCallback:) keyEquivalent: @""];
		
		(*env)->ReleaseStringUTFChars(env, text, buffer);
	}
}