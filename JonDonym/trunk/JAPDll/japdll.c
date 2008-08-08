/*
 * Funktionalität:
 * - ein mit Titel bezeichnetes Fenster "always on top" setzen und umgekehrt
 * - ein mit Titel bezeichnetes Fenster in den Taskbar schicken,
 *   Pfad zum Icon dafür in JAPICON festlegen
 * - das Fenster durch einen Klick auf das Icon im Taksbar wieder sichtbar machen,
 *   Icon verschwindet
 */
#if _MSC_VER > 1000
	#pragma once
#endif // _MSC_VER > 1000

//#define WIN32_LEAN_AND_MEAN		// Selten benutzte Teile der Windows-Header nicht einbinden
#define _WIN32_WINDOWS 0x0400 //Win95 compatible
#define WINVER 0x0400
//We do not need all this...
#define NOATOM
#define NOGDICAPMASKS
#define NOMETAFILE
#define NOMINMAX
#define NOOPENFILE
#define NORASTEROPS
#define NOSCROLL
#define NOSOUND
#define NOSYSMETRICS
#define NOTEXTMETRIC
#define NOWH
#define NOCOMM
#define NOKANJI
#define NOCRYPT
#define NOMCX

// Fügen Sie hier Ihre Header-Dateien ein
#include <windows.h>
#define NOTIFYICONDATA_SIZE NOTIFYICONDATA_V1_SIZE 

#include "japdll_jni.h"
#include "resource.h"
#include "dllversion.h"
#define WM_TASKBAREVENT WM_USER+100
#define WM_TASKBARCREATED (UINT)RegisterWindowMessage(TEXT("TaskbarCreated"))
#define BLINK_RATE 500

typedef BOOL (CALLBACK* LPChangeWindowMessageFilter)(UINT,DWORD);
#ifndef MSGFLT_ADD
	#define MSGFLT_ADD 1
#endif
#ifndef MSGFLT_REMOVE
	#define MSGFLT_REMOVE 2
#endif
UINT msgIconReset = 0;


struct t_find_window_by_name
	{
		const char * name;
		HWND hWnd;
	};
	

const char* JAVA_INTERFACE_CLASSNAME = "gui/JAPDll";

// globales Window Handle --> if !=null --> JAP is minimized
HWND g_hWnd=NULL;

//globales Handle fuer das Msg Window
HWND g_hMsgWnd=NULL;


// globales Moule Handle
HINSTANCE hInstance;

//globale Icon Handles...
HICON g_hiconJAP;
HICON g_hiconJAPBlink;
HICON g_hiconWindowSmall;
HICON g_hiconWindowLarge;

// Variable zur Sicherung der "alten" WndProc
//WNDPROC g_lpPrevWndFunc;

HANDLE g_hThread; //Handle for the Blinking-Thread
BOOL g_isBlinking;
BOOL bPopupClosed = TRUE;
VOID ShowWindowFromTaskbar(BOOL) ;
VOID showPopupMenu();
VOID closePopupMenu();
VOID doJavaCall(char* a_method);
BOOL setTooltipText(const char*, BOOL);

//Stores the Filename of the DLL
char strModuleFileName[4100];

JavaVM *gjavavm=NULL;

#pragma warning(disable:4100) //unref parameters
DWORD WINAPI MsgProcThread( LPVOID lpParam ) 
	{
		MSG msg;
		g_hMsgWnd=CreateWindow("JAPDllWndClass",NULL,0,CW_USEDEFAULT,CW_USEDEFAULT,CW_USEDEFAULT,CW_USEDEFAULT,NULL,NULL,hInstance,NULL);
		if(g_hMsgWnd==NULL)
			{
				return FALSE;
			}
		while(GetMessage(&msg,NULL,0,0))
			{
				TranslateMessage(&msg);
				DispatchMessage(&msg);
			}
		return TRUE;
	}
#pragma warning(default:4100)


/*
 * Neue WndProc zum Handeln von Ereignissen, besonders WM_TASKBAREVENT, 
 * das gesendet wird wenn etwas an "unserem" Icon im Taskbar passiert.
 */
#pragma warning(disable:4100) //unref parameters
LRESULT CALLBACK WndProc(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam)
#pragma warning(default:4100)
	{
		if (msg == WM_TASKBARCREATED && g_hWnd != NULL)
		{
			// explorer has recovered from a crash; reinstantiate the icon
			setTooltipText("JonDo", TRUE);
		}
		else if (msg == WM_TASKBAREVENT)
		{
			if (lParam == WM_LBUTTONUP || lParam == WM_RBUTTONUP || lParam == WM_MBUTTONUP)
			{
				closePopupMenu();
			}
			if(lParam==WM_LBUTTONDBLCLK)
			{
 				ShowWindowFromTaskbar(TRUE);
 			}
 			else if (lParam == WM_RBUTTONUP)
 			{
 				showPopupMenu();
 			}
			return 0;
		}  

		return DefWindowProc(hwnd,msg,wParam,lParam);
	}




BOOL createMsgWindowClass()
	{
		WNDCLASSEX wndclass;
		wndclass.lpszMenuName=NULL;
		wndclass.cbSize=sizeof(wndclass);
		wndclass.lpfnWndProc=WndProc;
		wndclass.cbClsExtra=0;
		wndclass.cbWndExtra=0;
		wndclass.hInstance=hInstance;
		wndclass.hIcon=NULL;
		wndclass.hbrBackground=(HBRUSH)GetStockObject(WHITE_BRUSH);
		wndclass.hCursor=LoadCursor(NULL,IDC_ARROW);
		wndclass.hIconSm=NULL;
		wndclass.lpszClassName="JAPDllWndClass";
		wndclass.style=0;
		// register task bar restore event after crash
		MyChangeWindowMessageFilter(WM_TASKBARCREATED, MSGFLT_ADD); 
		return (RegisterClassEx(&wndclass)!=0);
	}

#pragma warning(disable:4100) //unref parameters
BOOL APIENTRY DllMain( HINSTANCE hModule, 
                       DWORD  ul_reason_for_call, 
                       LPVOID lpReserved)
	{
		int ret=0;
    switch (ul_reason_for_call)
			{
				case DLL_PROCESS_ATTACH:
					hInstance=hModule;
					g_hThread=NULL;
					g_hWnd=NULL;
					g_hMsgWnd=NULL;
					//g_hiconJAP=(HICON)LoadImage(hInstance,MAKEINTRESOURCE(IDI_JAP),IMAGE_ICON,16,16,LR_DEFAULTCOLOR);
					g_hiconJAPBlink=(HICON)LoadImage(hInstance,MAKEINTRESOURCE(IDI_JAP_BLINK),IMAGE_ICON,16,16,LR_DEFAULTCOLOR);
					g_hiconWindowSmall=(HICON)LoadImage(hInstance,MAKEINTRESOURCE(IDI_JAP),IMAGE_ICON,16,16,LR_DEFAULTCOLOR);
					g_hiconWindowLarge=(HICON)LoadImage(hInstance,MAKEINTRESOURCE(IDI_JAP),IMAGE_ICON,32,32,LR_DEFAULTCOLOR);
					g_hiconJAP=g_hiconWindowSmall;
					ret=GetModuleFileName(hModule,strModuleFileName,4096);
					if(ret==0||ret>=4096)
						{
							strModuleFileName[0]=0;
						}
					return createMsgWindowClass();
				case DLL_THREAD_ATTACH:
				break;
				case DLL_THREAD_DETACH:
				break;
				case DLL_PROCESS_DETACH:	
				break;
			}
    return TRUE;
	}
#pragma warning(default:4100)



VOID showPopupMenu()
{
	JNIEnv* env=NULL;
	if (!bPopupClosed)
	{
		return;
	}
	if(gjavavm!=NULL)
	{
		(*gjavavm)->AttachCurrentThread(gjavavm,&env,NULL);
//		gjavavm->AttachCurrentThread(&env,NULL);
		if(env!=NULL)
		{
			jclass clazz=(*env)->FindClass(env,JAVA_INTERFACE_CLASSNAME);
			if(clazz!=NULL)
				{
					jmethodID mid=(*env)->GetStaticMethodID(env,clazz,"showPopupMenu","(JJ)J");
					if(mid!=NULL)
					{
						POINT position;
						jvalue args[2];
						bPopupClosed = FALSE;
					
						GetCursorPos(&position); 
						
						args[0].j = position.x;
						args[1].j = position.y;
						(*env)->CallStaticVoidMethodA(env,clazz,mid,args);
					}
				}
		}
		(*gjavavm)->DetachCurrentThread(gjavavm);
	}
}


VOID doJavaCall(char* a_method)
{
	JNIEnv* env=NULL;
	if(gjavavm!=NULL)
	{
		(*gjavavm)->AttachCurrentThread(gjavavm,&env,NULL);
		if(env!=NULL)
		{
			jclass clazz=(*env)->FindClass(env,JAVA_INTERFACE_CLASSNAME);
			if(clazz!=NULL)
				{
					jmethodID mid=(*env)->GetStaticMethodID(env,clazz,a_method,"()J");
					if(mid!=NULL)
					{
						(*env)->CallStaticVoidMethodA(env,clazz,mid,NULL);
					}
				}
		}
		(*gjavavm)->DetachCurrentThread(gjavavm);
	}
}

VOID closePopupMenu()
{
	doJavaCall("closePopupMenu");
}


void hideSystray()
{
	NOTIFYICONDATA nid;
	nid.hWnd = g_hMsgWnd;
	nid.cbSize = NOTIFYICONDATA_SIZE;
	nid.uID = IDI_JAP;
	nid.uFlags = 0;

	Shell_NotifyIcon(NIM_DELETE, &nid);
	DestroyWindow(g_hMsgWnd);
	g_hMsgWnd=NULL;
	g_hWnd=NULL;
}

/*
 * Zeigt g_hWnd (wieder) und entfernt das Icon aus dem Taskbar.
 * (Vorher sollte HideWindowInTaskbar aufgerufen worden sein)
 */
VOID ShowWindowFromTaskbar(BOOL a_bCallJavaShowMainWindow) 
	{
		if(g_hWnd==NULL)
			return;
		SetWindowPos(g_hWnd, HWND_TOP, 0, 0, 0, 0, SWP_NOSIZE|SWP_NOMOVE/*|SWP_SHOWWINDOW*/);
		
		//ShowWindow(g_hWnd, SW_SHOWNORMAL);
		if (a_bCallJavaShowMainWindow)
		{
			doJavaCall("showMainWindow");
		}
		ShowWindow(g_hWnd,SW_SHOWNORMAL);
		// Icondaten vorbereiten
		hideSystray();
	}
	

/*
 * Versteckt g_hWnd und erzeugt ein Icon im Taskbar.
 */
BOOL HideWindowInTaskbar(HWND hWnd,JNIEnv * env)
{
	DWORD dwThreadId;
	int i=10;
	g_hMsgWnd=NULL;
	if(hWnd==NULL)
	{
		return FALSE;
	}

	//Warten falls g_hWnd noch von letzten Aufruf "blockiert"
	while(g_hWnd!=NULL&&i>0)
		{
			Sleep(100);
			i--;
		}
	if(g_hWnd!=NULL)
		return FALSE;
	CreateThread(NULL, 0, MsgProcThread, NULL, 0, &dwThreadId); 
	i=10;
	while(g_hMsgWnd==NULL&&i>0)
		{
			Sleep(50);
			i--;
		}
	if(g_hMsgWnd==NULL)
		return FALSE;
		
	//connect to VM
	(*env)->GetJavaVM(env,&gjavavm);	
	


	// Window verstecken
	
	//jmethodID mid=env->GetStaticMethodID(clazz,"hiddeMainWindow","()J");
	//if(mid!=NULL)
	//	env->CallStaticVoidMethodA(clazz,mid,NULL);
	//else
	//	ShowWindow(hWnd, SW_HIDE);
	//Icon im Taskbar setzen
	if(setTooltipText("JonDo", TRUE)!=TRUE)
		{
			DestroyWindow(g_hMsgWnd);
			g_hMsgWnd=NULL;
			ShowWindow(hWnd, SW_SHOW);
			return FALSE;
		}
	g_hWnd=hWnd;
	return TRUE;
}

BOOL setTooltipText(const char* a_tooltip, BOOL a_bAdd)
{
	NOTIFYICONDATA nid;
	if (g_hMsgWnd == NULL)
	{
		return FALSE;
	}

	// Icondaten vorbereiten
	nid.hWnd = g_hMsgWnd;//hWnd;
	nid.cbSize = NOTIFYICONDATA_SIZE;
	nid.uID = IDI_JAP;
	nid.uFlags = NIF_MESSAGE | NIF_TIP | NIF_ICON;
	nid.uCallbackMessage = WM_TASKBAREVENT;
	lstrcpy(nid.szTip, a_tooltip);
	nid.hIcon = g_hiconJAP;
	if (a_bAdd)
	{
		return Shell_NotifyIcon(NIM_ADD, &nid);
	}
	else
	{
		return Shell_NotifyIcon(NIM_MODIFY, &nid);
	}
}

/*
 * Versteckt g_hWnd und erzeugt ein Icon im Taskbar.
 */
BOOL SetWindowIcon(HWND hWnd) 
{
	if(hWnd==NULL)
		return FALSE;
	return SendMessage( 
  hWnd,              // handle to destination window 
  WM_SETICON,               // message to send
  ICON_BIG,          // icon type
  (LPARAM)g_hiconWindowLarge           // handle to icon (HICON)
) &&	SendMessage( 
  hWnd,              // handle to destination window 
  WM_SETICON,               // message to send
  ICON_SMALL,          // icon type
  (LPARAM)g_hiconWindowSmall           // handle to icon (HICON)
);

}

/*
 * Von EnumWindow aufgerufene Prozedur zum Finden eines Fensters anhand seines Titels.
 * Liefert false (EnumWindow bricht ab) wenn das Fenster gefunden wurde und setzt g_hWnd, 
 * liefert true andernfalls (EnumWindows iteriert weiter).
 */
BOOL CALLBACK FindWindowByCaption(HWND hWnd, LPARAM lParam) 
{
	char caption[255];
	struct t_find_window_by_name* pFindWindow=(struct t_find_window_by_name*)lParam;
	
	if(GetWindowText(hWnd, caption, 255)==0)
		return TRUE;

	if (lstrcmp(pFindWindow->name,caption) == 0) 
		{
			pFindWindow->hWnd = hWnd;
			return FALSE;
		} 
	return TRUE;
}

/**
 * This function is only available - and needed - under Windows Vista.
 * Older Windows versions just ignore it.
 */
BOOL MyChangeWindowMessageFilter(UINT message, DWORD dwFlag)
{
	HINSTANCE hDLL;               // Handle to DLL
	LPChangeWindowMessageFilter fChangeWindowMessageFilter;    // Function pointer
	BOOL retVal = FALSE;

	hDLL = LoadLibrary("user32");
	if (hDLL != NULL)
	{
	   fChangeWindowMessageFilter = 
		   (LPChangeWindowMessageFilter)GetProcAddress(hDLL, "ChangeWindowMessageFilter");
	   if (!fChangeWindowMessageFilter)
	   {
		  // handle the error
		  FreeLibrary(hDLL);
	   }
	   else
	   {		 		   
		  // call the function
		  retVal = fChangeWindowMessageFilter(message, dwFlag);
	   }
	}

	return retVal;
}


#pragma warning(disable:4100) //unref parameters
DWORD WINAPI BlinkThread( LPVOID lpParam ) 
	{ 
		NOTIFYICONDATA nid;
		nid.hWnd = g_hMsgWnd;
		nid.cbSize = NOTIFYICONDATA_SIZE;
		nid.uID = IDI_JAP;
		nid.uFlags = NIF_ICON;
		while (g_isBlinking) 
			{
				g_isBlinking = FALSE;
				nid.hIcon = g_hiconJAPBlink;
				Shell_NotifyIcon(NIM_MODIFY, &nid);
				Sleep(BLINK_RATE);
				if(g_hMsgWnd==NULL)
					break;
				nid.hIcon = g_hiconJAP;
				Shell_NotifyIcon(NIM_MODIFY, &nid);
				Sleep(BLINK_RATE);
				if(g_hMsgWnd==NULL)
					break;
				nid.hIcon = g_hiconJAPBlink;
				Shell_NotifyIcon(NIM_MODIFY, &nid);
				Sleep(BLINK_RATE);
				if(g_hMsgWnd==NULL)
					break;
				nid.hIcon = g_hiconJAP;
				Shell_NotifyIcon(NIM_MODIFY, &nid);
				Sleep(BLINK_RATE);
			}
			g_hThread=NULL;
			return 0;
} 
#pragma warning(default:4100)

VOID OnTraffic() 
{
  DWORD dwThreadId;
	g_isBlinking = TRUE;
	if (g_hThread == NULL) 
		{
			g_hThread = CreateThread(NULL, 0, BlinkThread, NULL, 0, &dwThreadId);                
		}
}



/************************************************************************************
									JNI Methoden
 ************************************************************************************/
#pragma warning(disable:4100) //unref parameters
JNIEXPORT void JNICALL Java_gui_JAPDll_setWindowOnTop_1dll
  (JNIEnv * env, jclass c, jstring s, jboolean b)
{
	struct t_find_window_by_name tmp;
	tmp.name=(*env)->GetStringUTFChars(env,s, 0);
	tmp.hWnd=NULL;
	EnumWindows(&FindWindowByCaption, (LPARAM) &tmp);
	if (tmp.hWnd != NULL) 
		{
			if (b) 
				SetWindowPos(tmp.hWnd, HWND_TOPMOST, 0, 0, 0, 0, SWP_NOACTIVATE | SWP_NOMOVE | SWP_NOSIZE);
			else
				SetWindowPos(tmp.hWnd, HWND_NOTOPMOST, 0, 0, 0, 0, SWP_NOACTIVATE | SWP_NOMOVE | SWP_NOSIZE);
		}
	(*env)->ReleaseStringUTFChars(env,s, tmp.name);	
	return;
}
#pragma warning(default:4100)

#pragma warning(disable:4100) //unref parameters
JNIEXPORT jboolean JNICALL Java_gui_JAPDll_setTooltipText_1dll
  (JNIEnv * env, jclass c, jstring s)
{
	//env->GetStringUTFChars(s, 0);
	//return FALSE;
	return (jboolean)setTooltipText((*env)->GetStringUTFChars(env,s, 0), FALSE);
}
#pragma warning(default:4100)

#pragma warning(disable:4100) //unref parameters
JNIEXPORT jboolean JNICALL Java_gui_JAPDll_hideWindowInTaskbar_1dll
  (JNIEnv * env, jclass javaclass, jstring s)
{
	struct t_find_window_by_name tmp;
	jboolean ret=TRUE;
	tmp.name=(*env)->GetStringUTFChars(env,s, 0);
	tmp.hWnd=NULL;
	EnumWindows(&FindWindowByCaption, (LPARAM) &tmp);
	if (tmp.hWnd!= NULL) 
		ret=(jboolean)HideWindowInTaskbar(tmp.hWnd,env);
	else
		ret=FALSE;
	(*env)->ReleaseStringUTFChars(env,s,tmp.name);
	return ret;
}
#pragma warning(default:4100)

#pragma warning(disable:4100) //unref parameters
JNIEXPORT jboolean JNICALL Java_gui_JAPDll_setWindowIcon_1dll (JNIEnv* env, jclass c, jstring s)
{
	struct t_find_window_by_name tmp;
	jboolean ret=TRUE;
	tmp.name=(*env)->GetStringUTFChars(env,s, 0);
	tmp.hWnd=NULL;
	EnumWindows(&FindWindowByCaption, (LPARAM) &tmp);
	if (tmp.hWnd!= NULL) 
	{
		ret=(jboolean)SetWindowIcon(tmp.hWnd);
	}
	else
		ret=FALSE;
	(*env)->ReleaseStringUTFChars(env,s,tmp.name);
	return ret;
}
#pragma warning(default:4100)

#pragma warning(disable:4100) //unref parameters
JNIEXPORT void JNICALL Java_gui_JAPDll_onTraffic_1dll
  (JNIEnv * env, jclass c) 
{
	if (g_hWnd != NULL) 
	{
		OnTraffic();
	}
}
#pragma warning(default:4100)

#pragma warning(disable:4100) //unref parameters
JNIEXPORT void JNICALL Java_gui_JAPDll_popupClosed_1dll
  (JNIEnv * env, jclass c) 
{
	if (g_hWnd != NULL) 
	{
		bPopupClosed = TRUE;
	}
}
#pragma warning(default:4100)

#pragma warning(disable:4100) //unref parameters
JNIEXPORT void JNICALL Java_gui_JAPDll_hideSystray_1dll
  (JNIEnv * env, jclass c) 
{
	hideSystray();
}
#pragma warning(default:4100)

#pragma warning(disable:4100) //unref parameters
JNIEXPORT void JNICALL Java_gui_JAPDll_showWindowFromTaskbar_1dll
  (JNIEnv * env, jclass c) 
{
	ShowWindowFromTaskbar(FALSE);
}
#pragma warning(default:4100)

#pragma warning(disable:4100) //unref parameters
JNIEXPORT jstring JNICALL Java_gui_JAPDll_getDllVersion_1dll
  (JNIEnv * env, jclass c)
	{		
		return (*env)->NewStringUTF(env,JAPDLL_VERSION);
	}
#pragma warning(default:4100)

#pragma warning(disable:4100) //unref parameters
JNIEXPORT jstring JNICALL Java_gui_JAPDll_getDllFileName_1dll
  (JNIEnv * env, jclass c)
	{
		return (*env)->NewStringUTF(env,strModuleFileName);
	}
#pragma warning(default:4100)
