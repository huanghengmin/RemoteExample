package com.zd.vpn.remote;

import android.app.Activity;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.zd.vpn.api.APIVpnProfile;
import com.zd.vpn.api.IOpenVPNAPIService;
import com.zd.vpn.api.IOpenVPNStatusCallback;

import java.util.List;

public class MainFragment extends Fragment implements View.OnClickListener, Handler.Callback {

    private TextView mStatusRequest;
    private EditText mServerIp;
    private EditText mServerPort;
    private EditText mServerStrategyPort;
    private Button mInit;
    private Button mLoadCert;
    private Button mStartZdVpn;
    private Button mStopZdVpn;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_main, container, false);
        mStatusRequest = (TextView) v.findViewById(R.id.status_request);
        mServerIp = (EditText)v.findViewById(R.id.server_ip);
        mServerPort = (EditText)v.findViewById(R.id.server_port);
        mServerStrategyPort = (EditText)v.findViewById(R.id.server_strategy_port);
        mInit = (Button) v.findViewById(R.id.init);
        mInit.setOnClickListener(this);
        mLoadCert = (Button) v.findViewById(R.id.loadCert);
        mLoadCert.setOnClickListener(this);
        mStartZdVpn = (Button) v.findViewById(R.id.startZDVPN);
        mStartZdVpn.setOnClickListener(this);
        mStopZdVpn = (Button) v.findViewById(R.id.stopZDVPN);
        mStopZdVpn.setOnClickListener(this);
        return v;

    }

    private static final int MSG_UPDATE_STATE = 0;
    private static final int MSG_UPDATE_MYIP = 1;
    private static final int ICS_OPENVPN_PERMISSION = 7;

    private static final int INIT_ZDVPN = 2;
    private static final int LOAD_ZDVPN = 3;
    private static final int START_ZDVPN = 4;
    private static final int STOP_ZDVPN = 5;

    protected IOpenVPNAPIService mService=null;
    private Handler mHandler;


    private void startZDVPN()
    {
        try {
             String msg =  mService.startZDVPN();
             mStatusRequest.setText("start result:::::::::::::::::::"+msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void stopZDVPN()
    {
        try {
            mService.stopZDVPN();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
    
    
    private void loadZDVPN() {
    	try {
			String[] strings = mService.loadCert();
			StringBuilder sBuilder = new StringBuilder();
			for (int i = 0; i < strings.length; i++) {
				sBuilder.append(strings[i]+"   ");
				mStatusRequest.setText("LoadCert::::::::::::::::"+sBuilder.toString());
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	private void initZDVPN() {
		try {
			
			  String server_ip = mServerIp.getText().toString();
              String server_port = mServerPort.getText().toString();
              String server_strategy_port = mServerStrategyPort.getText().toString();
			
			 if (server_ip.isEmpty()|| 
					 server_ip == null || 
					 server_port.isEmpty()|| 
					 server_port == null || 
					 server_strategy_port.isEmpty()|| 
					 server_strategy_port == null ) {
				 mStatusRequest.setText("init_flag::::::::::::::::::: plase input params");
			 }else {
				 boolean init = mService.init(server_ip, Integer.parseInt(server_port), Integer.parseInt(server_strategy_port),
                         "111111", "KingTrustVPN", false);
				    if(init){
                        mStatusRequest.setText("init_flag:::::::::::::::::::"+init);
				    }else {
				    	mStatusRequest.setText("init_flag:::::::::::::::::::"+init);
					}
			}
			
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

    @Override
    public void onStart() {
        super.onStart();
        mHandler = new Handler(this);
        bindService();
    }


    private IOpenVPNStatusCallback mCallback = new IOpenVPNStatusCallback.Stub() {

        @Override
        public void newStatus(String uuid, String state, String message, String level)
                throws RemoteException {
            Message msg = Message.obtain(mHandler, MSG_UPDATE_STATE, state + "|" + message);
            msg.sendToTarget();

        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            mService = IOpenVPNAPIService.Stub.asInterface(service);

            try {
                Intent i = mService.prepare(getActivity().getPackageName());
                if (i!=null) {
                    startActivityForResult(i, ICS_OPENVPN_PERMISSION);
                } else {
                    onActivityResult(ICS_OPENVPN_PERMISSION, Activity.RESULT_OK,null);
                }

            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;

        }
    };

    private void bindService() {
        Intent intent =new Intent(IOpenVPNAPIService.class.getName());
        intent.setPackage("com.zd.vpn");
        getActivity().bindService(intent,
                mConnection, Context.BIND_AUTO_CREATE);
    }

    protected void listVPNs() {

        try {
            List<APIVpnProfile> list = mService.getProfiles();
            String all="List:";
            for(APIVpnProfile vp:list.subList(0, Math.min(5, list.size()))) {
                all = all + vp.mName + ":" + vp.mUUID + "\n";
            }

            if (list.size() > 5)
                all +="\n And some profiles....";

           mStatusRequest.setText(all);

        } catch (RemoteException e) {
        	mStatusRequest.setText(e.getMessage());
        }
    }

    private void unbindService() {
        getActivity().unbindService(mConnection);
    }

    @Override
    public void onStop() {
        super.onStop();
        unbindService();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.init:
                try {
                    prepareStartProfile(INIT_ZDVPN);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
                
            case R.id.loadCert:
                try {
                    prepareStartProfile(LOAD_ZDVPN);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
                
            case R.id.startZDVPN:
                try {
                    prepareStartProfile(START_ZDVPN);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
                
            case R.id.stopZDVPN:
                try {
                    prepareStartProfile(STOP_ZDVPN);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }

    }

    private void prepareStartProfile(int requestCode) throws RemoteException {
        Intent requestpermission = mService.prepareVPNService();
        if(requestpermission == null) {
            onActivityResult(requestCode, Activity.RESULT_OK, null);
        } else {
            startActivityForResult(requestpermission, requestCode);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
        	  if(requestCode==INIT_ZDVPN)
                  initZDVPN();
        	  if(requestCode==LOAD_ZDVPN)
                  loadZDVPN();
            if(requestCode==START_ZDVPN)
                startZDVPN();
            if(requestCode==STOP_ZDVPN)
                stopZDVPN();
            if (requestCode == ICS_OPENVPN_PERMISSION) {
                listVPNs();
                try {
                    mService.registerStatusCallback(mCallback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    };


    @Override
    public boolean handleMessage(Message msg) {
        if(msg.what == MSG_UPDATE_STATE) {
            mStatusRequest.setText((CharSequence) msg.obj);
        } 
        return true;
    }
}