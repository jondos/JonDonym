#import "JAPMacOSXLib.h"

static JavaVM *jvm;
static jmethodID dockMenuCallback_mID;
static jmethodID showDockMenu_mID;
static jclass jDelegateClass;

jclass jMenuClass;
jclass jMenuItemClass;
jclass jCheckBoxMenuItemClass;
jclass jSeparatorClass;
	
jmethodID getMenuComponentCount_mID;
jmethodID getMenuComponent_mID;
jmethodID getText_mID;
jmethodID getActionCommand_mID;
jmethodID isSelected_mID;

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
	JNIEnv *env = NULL;
	bool detach = false;
	
	if(GetJNIEnv(&env, &detach) != JNI_OK)
	{
		NSLog(@"dockMenuCallback: could not attack to JVM");
		return NULL;
	}
	
	jobject obj = (*env)->CallStaticObjectMethod(env, jDelegateClass, showDockMenu_mID);
	
	NSMenu* menu = getNSMenuFromJMenu(env, obj);
	
	if(detach)
	{
		(*jvm)->DetachCurrentThread(jvm);
	}
	
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
	
	NSString* cmd = [sender representedObject];
	
	/*jsize length = [cmd length];
	jchar buffer[length];
	
	[cmd getCharacters:(unichar *)buffer];
			// fill the jobjectArray
			jstring js = (*env)->NewString(env, buffer, buflen);*/
	const char* buffer = [cmd UTF8String];
	jstring string = (*env)->NewStringUTF(env, buffer);
	
	(*env)->CallStaticVoidMethod(env, jDelegateClass, dockMenuCallback_mID, string);
	
	(*env)->DeleteLocalRef(env, string);
	
	if(detach)
	{
		(*jvm)->DetachCurrentThread(jvm);
	}
}
@end

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) 
{
	jvm = vm;
	
	return JNI_VERSION_1_4;
}

//	% javap -classpath JavaHeaders.jar -s gui.JAPMacOSXLib
JNIEXPORT void JNICALL Java_gui_JAPMacOSXLib_nativeInit
  (JNIEnv * env, jclass clazz) 
{
	jDelegateClass = (*env)->NewGlobalRef(env, clazz);
	dockMenuCallback_mID = (*env)->GetStaticMethodID(env, clazz, "dockMenuCallback", "(Ljava/lang/String;)V");
	showDockMenu_mID = (*env)->GetStaticMethodID(env, clazz, "showDockMenu", "()Ljavax/swing/JMenu;");
	
	jMenuClass = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "javax/swing/JMenu"));
	jMenuItemClass = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "javax/swing/JMenuItem"));
	jCheckBoxMenuItemClass = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "javax/swing/JCheckBoxMenuItem"));	
	jSeparatorClass = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "javax/swing/JSeparator"));
	
	getMenuComponentCount_mID = (*env)->GetMethodID(env, jMenuClass, "getMenuComponentCount", "()I");
	getMenuComponent_mID = (*env)->GetMethodID(env, jMenuClass, "getMenuComponent", "(I)Ljava/awt/Component;");
	getText_mID = (*env)->GetMethodID(env, jMenuItemClass, "getText", "()Ljava/lang/String;");
	getActionCommand_mID = (*env)->GetMethodID(env, jMenuItemClass, "getActionCommand", "()Ljava/lang/String;");
	isSelected_mID = (*env)->GetMethodID(env, jMenuItemClass, "isSelected", "()Z");
	
	if(getMenuComponentCount_mID == 0)
	{
		NSLog(@"getNSMenuFromJMenu: could not find method JMenu_getMenuComponentCount");
		return;
	}
	
	if(getMenuComponent_mID == 0)
	{
		NSLog(@"getNSMenuFromJMenu: could not find method JMenu_getMenuComponent");
		return;
	}

	if(getText_mID == 0)
	{
		NSLog(@"getNSMenuFromJMenu: could not find method JMenuItem_getText");
		return;
	}
	
	if(getActionCommand_mID == 0)
	{
		NSLog(@"getNSMenuFromJMenu: could not find method JMenuItem_getActionCommand");
		return;
	}
	
	if(isSelected_mID == 0)
	{
		NSLog(@"getNSMenuFromJMenu: could not find method JMenuItem_isSelected");
		return;
	}
}

JNIEXPORT void JNICALL Java_gui_JAPMacOSXLib_nativeInitDockMenu
  (JNIEnv * env, jclass class)
{
	NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
	
  	[NSApplication sharedApplication];
	
	[NSApp setDelegate: [AppDelegate new]];
	
	[pool release];
}

NSMenu* getNSMenuFromJMenu(JNIEnv* env, jobject obj)
{
	NSMenu* menu = [NSMenu new];
	[menu retain];
	

	
	if(!(*env)->IsInstanceOf(env, obj, jMenuClass) == JNI_TRUE)
	{
		NSLog(@"getNSMenuFromJMenu: object is not a JMenu");
		return NULL;
	}
	

	
	jint count = (*env)->CallIntMethod(env, obj, getMenuComponentCount_mID);
	jint i;
	
	for(i = 0; i < count; i++)
	{
		jobject item = (*env)->CallObjectMethod(env, obj, getMenuComponent_mID, i);
		
		if(item == NULL)
		{
			NSLog(@"getNSMenuFromJMenu: unknown error. skipping menu entry %i", i);
			continue;
		}
		
		if((*env)->IsInstanceOf(env, item, jMenuItemClass) == JNI_TRUE)
		{
			jstring text = (jstring) (*env)->CallObjectMethod(env, item, getText_mID);
			jstring cmd = (jstring) (*env)->CallObjectMethod(env, item, getActionCommand_mID);
			const char* buffer = (*env)->GetStringUTFChars(env, text, NULL);
			const char* cmdBuffer = (*env)->GetStringUTFChars(env, cmd, NULL);
			
			NSString* string = [NSString stringWithUTF8String: buffer];
			NSString* cmdString = [NSString stringWithUTF8String: cmdBuffer];
			
			if((*env)->IsInstanceOf(env, item, jMenuClass) == JNI_TRUE)
			{
				NSLog(@"getNSMenuFromJMenu: adding submenu");
				NSMenu* submenu = getNSMenuFromJMenu(env, item);
				[menu setSubmenu: submenu forItem: [menu addItemWithTitle: string action:@selector(dockMenuCallback:) keyEquivalent:@""]]; 
			}
			else if((*env)->IsInstanceOf(env, item, jCheckBoxMenuItemClass) == JNI_TRUE)
			{
				NSLog(@"getNSMenuFromJMenu: adding checkbox item");
				NSMenuItem* menuItem = [menu addItemWithTitle: string action: @selector(dockMenuCallback:) keyEquivalent: @""];
				jboolean isSelected = (*env)->CallBooleanMethod(env, item, isSelected_mID);
				[menuItem setRepresentedObject:cmdString];
				if(isSelected == JNI_TRUE)
				{
					[menuItem setState:NSOnState];
				}
				else
				{
					[menuItem setState:NSOffState];
				}
			}
			else
			{
				NSLog(@"getNSMenuFromJMenu: adding menu item '%s' - command: %s", buffer, cmdBuffer);
				NSMenuItem* menuItem = [menu addItemWithTitle: string action: @selector(dockMenuCallback:) keyEquivalent: @""];
				[menuItem setRepresentedObject:cmdString];
			}
			
			(*env)->ReleaseStringUTFChars(env, text, buffer);
			(*env)->ReleaseStringUTFChars(env, text, cmdBuffer);
		}
		else if((*env)->IsInstanceOf(env, item, jSeparatorClass) == JNI_TRUE)
		{
			NSLog(@"getNSMenuFromJMenu: adding separator");
			[menu addItem:[NSMenuItem separatorItem]];
		}
	}
	
	return menu;
}